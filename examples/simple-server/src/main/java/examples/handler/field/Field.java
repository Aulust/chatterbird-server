package examples.handler.field;

import chatterbird.server.engine.EventHandler;
import chatterbird.server.engine.Handler;
import chatterbird.server.engine.QueueHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@QueueHandler(name = "field")
public class Field extends Handler {
  private AtomicLong idGenerator = new AtomicLong();
  private ConcurrentHashMap<String, Long> players = new ConcurrentHashMap<String, Long>();

  @Autowired
  private ObjectMapper objectMapper;

  @EventHandler(event = "connect")
  public void connect(String sessionId, JsonNode data) {
    Long playerId = idGenerator.incrementAndGet();
    players.put(sessionId, playerId);

    sendMessage(sessionId, "connected", objectMapper.valueToTree(playerId));
    sendMessageConnected("newPlayer", objectMapper.valueToTree(playerId));
  }

  @EventHandler(event = "disconnect")
  public void disconnect(String sessionId, JsonNode data) {
    Long playerId = players.remove(sessionId);
    sendMessageConnected("disconnected", objectMapper.valueToTree(playerId));
  }

  @EventHandler(event = "move")
  public void move(String sessionId, JsonNode data) {
    sendMessageConnected("move", data);
  }
}
