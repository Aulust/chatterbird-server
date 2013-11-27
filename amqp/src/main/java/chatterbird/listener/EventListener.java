package chatterbird.listener;

import chatterbird.server.MessageConverter;
import chatterbird.server.engine.Engine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

import java.util.List;

@Component
public class EventListener implements ErrorHandler {
  private static final Logger logger = LoggerFactory.getLogger(EventListener.class);

  @Autowired
  private Engine engine;
  @Autowired
  private MessageConverter messageConverter;

  public void handleMessage(JsonNode data) {
    List<ObjectNode> messages = messageConverter.convert(data);

    if (messages != null) {
      for (ObjectNode message : messages) {
        engine.fireEvent(message.get("handler").asText(), message.get("event").asText(), message.get("data"));
      }
    }
  }

  @Override
  public void handleError(Throwable throwable) {
    logger.error("Error processing amqp message", throwable);
  }
}
