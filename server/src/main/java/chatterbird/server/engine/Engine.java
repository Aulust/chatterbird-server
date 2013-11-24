package chatterbird.server.engine;


import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class Engine {
  private static final Logger logger = LoggerFactory.getLogger(Engine.class);

  @Autowired
  Environment env;
  @Autowired
  private ApplicationContext applicationContext;
  @Autowired
  private ThreadPoolExecutor threadPoolExecutor;

  private ImmutableMap<String, Handler> handlers;
  private ImmutableMap<String, Map<String, Method>> eventHandlers;

  @PostConstruct
  public void loadHandlers() {
    Map<String, Handler> handlers = new HashMap<String, Handler>();
    Map<String, Map<String, Method>> eventHandlers = new HashMap<String, Map<String, Method>>();

    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AnnotationTypeFilter(QueueHandler.class));

    for (BeanDefinition component : provider.findCandidateComponents(env.getProperty("handler.package"))) {
      try {
        Class<? extends Handler> clazz = Class.forName(component.getBeanClassName()).asSubclass(Handler.class);
        String name = clazz.getAnnotation(QueueHandler.class).name();

        Map<String, Method> classEvents = new HashMap<String, Method>();
        for (Method method : clazz.getMethods()) {
          EventHandler annotation = method.getAnnotation(EventHandler.class);
          if (annotation != null) {
            classEvents.put(annotation.event(), method);
          }
        }

        eventHandlers.put(name, ImmutableMap.copyOf(classEvents));
        handlers.put(name, applicationContext.getBean(clazz));
        logger.info("Handler {} has been initialized with events {}", name, classEvents.keySet());
      } catch (ClassCastException e) {
        logger.error("QueueHandler annotation was used on class not implementing Handler interface", e);
      } catch (ClassNotFoundException e) {
        logger.error("Handler class {} not found", component.getBeanClassName(), e);
      } catch (NullPointerException e) {
        logger.error("Could not find bean for class {}", component.getBeanClassName(), e);
      }
    }

    this.handlers = ImmutableMap.copyOf(handlers);
    this.eventHandlers = ImmutableMap.copyOf(eventHandlers);
  }

  private Runnable getEventTask(final String handler, final String sessionId, final String event, final JsonNode data) {
    final Handler handlerClass = this.handlers.get(handler);
    final Method handlerMethod = this.eventHandlers.get(handler).get(event);

    if (handlerClass == null || handlerMethod == null) {
      return null;
    }

    return new Runnable() {
      @Override
      public void run() {
        try {
          handlerMethod.invoke(handlerClass, sessionId, data);
        } catch (IllegalAccessException e) {
          logger.error("Event handler method is not accessible {}", e);
        } catch (InvocationTargetException e) {
          logger.error("Error while processing task {}", e);
        }
      }
    };
  }

  public boolean isHandlerExists(String name) {
    return handlers.containsKey(name);
  }

  public void fireEvent(String handler, String event, JsonNode data) {
    fireEvent(null, handler, event, data);
  }

  public void fireEvent(String sessionId, String handler, String event) {
    fireEvent(sessionId, handler, event, null);
  }

  public void fireEvent(String sessionId, String handler, String event, JsonNode data) {
    Runnable task = getEventTask(handler, sessionId, event, data);

    if (task != null) {
      threadPoolExecutor.execute(task);
    }
  }

  public static class Events {
    public static final String CONNECT = "connect";
    public static final String DISCONNECT = "disconnect";

    public static boolean isReservedEvent(String event) {
      return CONNECT.equals(event) || DISCONNECT.equals(event);
    }
  }
}
