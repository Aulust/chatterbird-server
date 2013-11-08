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
  //@Autowired
  //private UserService userService;

  @Override
  public void newClient(String sessionId) {
    Thread.activeCount();
    sendMessage(sessionId, String.valueOf(sessionManager.sessions.size()));

	        //User user = new User(null, "K.siva reddy", "hyderabad");
	        //Integer id = userService.createUser(user);
	        //System.out.println("Newly created User Id="+id);
	        /*for (User u : userService.getAllUsers())
	        {
	            System.out.println(u);
	        }*/
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
