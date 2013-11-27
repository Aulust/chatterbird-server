package chatterbird.server.engine;

import chatterbird.server.SessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

public abstract class Handler {
  protected String name;
  @Autowired
  private SessionManager sessionManager;

  @PostConstruct
  private void init() {
    name = this.getClass().getAnnotation(QueueHandler.class).name();
  }

  protected void sendMessage(String sessionId, String event, JsonNode data) {
    sessionManager.sendMessage(name, sessionId, event, data);
  }

  protected void sendMessageConnected(String event, JsonNode message) {
    sessionManager.sendMessageConnected(name, event, message);
  }
}
