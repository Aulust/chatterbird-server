package examples.handler.voting;

import chatterbird.engine.AmqpAwareHandler;
import chatterbird.server.engine.EventHandler;
import chatterbird.server.engine.QueueHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import examples.service.VoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@QueueHandler(name = "voting")
public class Voting extends AmqpAwareHandler {
  @Autowired
  private VoteService voteService;
  @Autowired
  private ObjectMapper objectMapper;

  @EventHandler(event = "connect")
  public void connect(String sessionId, JsonNode data) {
    Map<String, Long> counts = voteService.countVotes();
    sendMessage(sessionId, "status", objectMapper.valueToTree(counts));
  }

  @EventHandler(event = "vote")
  public void vote(String sessionId, JsonNode data) {
    voteService.addVote(data.get("name").asText(), data.get("value").asBoolean());
    broadcastEvent("votesChanged");
  }

  @EventHandler(event = "votesChanged")
  public void votesChanged(String sessionId, JsonNode data) {
    Map<String, Long> counts = voteService.countVotes();
    sendMessageConnected("status", objectMapper.valueToTree(counts));
  }
}
