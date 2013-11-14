package chatterbird.server.engine;


import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class Engine {
  private static final Logger logger = LoggerFactory.getLogger(Engine.class);

  @Autowired
  private ApplicationContext applicationContext;
  @Autowired
  private ThreadPoolExecutor threadPoolExecutor;

  private ImmutableMap<String, Handler> handlers;

  @PostConstruct
  public void loadHandlers() {
    Map<String, Handler> handlers = new HashMap<String, Handler>();
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AnnotationTypeFilter(QueueHandler.class));

    for (BeanDefinition component : provider.findCandidateComponents("chatterbird.handler")) {
      try {
        Class<? extends Handler> clazz = Class.forName(component.getBeanClassName()).asSubclass(Handler.class);
        handlers.put(clazz.getAnnotation(QueueHandler.class).name(), applicationContext.getBean(clazz));
      } catch (ClassCastException e) {
        logger.error("QueueHandler annotation was used on class not implementing Handler interface", e);
      } catch (ClassNotFoundException e) {
        logger.error("Handler class {} not found", component.getBeanClassName(), e);
      } catch (NullPointerException e) {
        logger.error("Could not find bean for class {}", component.getBeanClassName(), e);
      }
    }

    this.handlers = ImmutableMap.copyOf(handlers);
  }

  private Runnable getNewClientTask(final Handler handler, final String sessionId) {
    return new Runnable() {
      @Override
      public void run() {
        handler.newClient(sessionId);
      }
    };
  }

  private Runnable getMessageTask(final Handler handler, final String sessionId, final String message) {
    return new Runnable() {
      @Override
      public void run() {
        handler.clientMessage(sessionId, message);
      }
    };
  }

  private Runnable getInternalMessageTask(final Handler handler, final String message) {
    return new Runnable() {
          @Override
          public void run() {
            handler.broadcastMessage(message);
          }
        };
  }

  public boolean isHandlerExists(String name) {
    return handlers.containsKey(name);
  }

  public void newClientEvent(String handler, String sessionId) {
    threadPoolExecutor.execute(getNewClientTask(this.handlers.get(handler), sessionId));
  }

  public void messageEvent(String handler, String sessionId, String message) {
    threadPoolExecutor.execute(getMessageTask(this.handlers.get(handler), sessionId, message));
  }

  public void internalMessageEvent(String handler, String message) {
    threadPoolExecutor.execute(getInternalMessageTask(this.handlers.get(handler), message));
  }
}
