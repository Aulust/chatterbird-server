package chatterbird.handler.admin;

import chatterbird.server.SessionManager;
import chatterbird.server.engine.Handler;
import chatterbird.server.engine.QueueHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@QueueHandler(name = "admin")
public class Admin extends Handler {
  @Autowired
  private SessionManager sessionManager;

  @Override
  public void newClient(String sessionId) {
    sendMessage(sessionId, String.valueOf(sessionManager.sessions.size()));
  }

  @Override
  public void clientMessage(String sessionId, String message) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void broadcastMessage(String message) {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}
