package examples.handler.field;

import chatterbird.server.engine.EventHandler;
import chatterbird.server.engine.Handler;
import chatterbird.server.engine.QueueHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
@QueueHandler(name = "field")
public class Field extends Handler {
  private AtomicLong id = new AtomicLong();
  @Autowired
  private ObjectMapper objectMapper;

  @EventHandler(event = "connect")
  public void connect(String sessionId, JsonNode data) {
    sendMessage(sessionId, "connected", objectMapper.valueToTree(id.incrementAndGet()));
  }

  @EventHandler(event = "move")
  public void move(String sessionId, JsonNode data) {
    sendMessageConnected("move", data);
  }
}
