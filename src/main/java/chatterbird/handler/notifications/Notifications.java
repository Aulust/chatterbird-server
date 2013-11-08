package chatterbird.handler.notifications;

import chatterbird.server.engine.Handler;
import chatterbird.server.engine.QueueHandler;
import org.springframework.stereotype.Component;

@Component
@QueueHandler(name = "notifications")
public class Notifications extends Handler {
  @Override
  public void newClient(String sessionId) {
  }

  @Override
  public void clientMessage(String sessionId, String message) {
  }

  @Override
  public void internalMessage(String message) {
    sendMessageConnected(message);
  }
}
