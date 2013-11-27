package chatterbird.engine;


import chatterbird.server.MessageConverter;
import chatterbird.server.engine.Handler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AmqpAwareHandler extends Handler {
  @Autowired
  private RabbitTemplate rabbitTemplate;
  @Autowired
  private MessageConverter messageConverter;
  @Autowired
  private ObjectMapper objectMapper;

  protected void broadcastEvent(String event) {
    broadcastEvent(event, null);
  }

  protected void broadcastEvent(String event, JsonNode data) {
    ObjectNode message = messageConverter.convert(name, event, data);
    rabbitTemplate.convertAndSend(objectMapper.createArrayNode().add(message));
  }
}
