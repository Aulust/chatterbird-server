package examples.handler.notifications;

import chatterbird.server.engine.EventHandler;
import chatterbird.server.engine.Handler;
import chatterbird.server.engine.QueueHandler;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
@QueueHandler(name = "notifications")
public class Notifications extends Handler {
  @EventHandler(event = "notification")
  public void notification(String sessionId, JsonNode data) {
    sendMessageConnected("notification", data);
  }
}
