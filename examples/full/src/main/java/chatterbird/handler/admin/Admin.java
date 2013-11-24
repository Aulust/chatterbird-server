package chatterbird.handler.admin;

import chatterbird.server.SessionManager;
import chatterbird.server.engine.Handler;
import chatterbird.server.engine.QueueHandler;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@QueueHandler(name = "admin")
public class Admin extends Handler {
  @Autowired
  private SessionManager sessionManager;
  @Autowired
  private UserService userService;

/*  @Override
  public void newClient(String sessionId) {
    Thread.activeCount();

	        User user = new User(null, "K.siva reddy", "hyderabad");
	        Integer id = userService.createUser(user);
	        System.out.println("Newly created User Id="+id);

  }
*/
}
