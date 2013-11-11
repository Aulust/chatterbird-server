package chatterbird.server.engine;

import chatterbird.server.SessionManager;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

public abstract class Handler {
  private String name;
  @Autowired
  private SessionManager sessionManager;

  public abstract void newClient(String sessionId);
  public abstract void clientMessage(String sessionId, String message);
  public abstract void broadcastMessage(String message);

  @PostConstruct
  private void init() {
    name = this.getClass().getAnnotation(QueueHandler.class).name();
  }

  protected void sendMessage(String sessionId, String message) {
    sessionManager.sendMessage(name, sessionId, message);
  }

  protected void sendMessageConnected(String message) {
    sessionManager.sendMessageConnected(name, message);
  }
}
