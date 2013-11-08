package chatterbird.handler.test;

import chatterbird.server.engine.Handler;
import chatterbird.server.engine.QueueHandler;
import org.springframework.stereotype.Component;

@Component
@QueueHandler(name = "test")
public class Test extends Handler {
  @Override
  public void newClient(String sessionId) {
    System.out.println("Test handler new client");
    System.out.println(sessionId);
    sendMessage(sessionId, "bla forever!");
  }

  @Override
  public void clientMessage(String sessionId, String message) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void internalMessage(String message) {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}
