package chatterbird.server.frame;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public abstract class InboundFrame {
  //TODO Get rid of this static shit
  private static ObjectMapper objectMapper;

  public String sessionId;

  public static void setObjectMapper(ObjectMapper objectMapper) {
    InboundFrame.objectMapper = objectMapper;
  }

  public static ConnectionFrame connectionFrame(String sessionId) {
    return new ConnectionFrame(sessionId);
  }

  public static CloseFrame closeFrame(String sessionId) {
    return new CloseFrame(sessionId);
  }

  public static MessageFrame messageFrame(String sessionId, String data) {
    Map<String, String> messages = new HashMap<String, String>();
    //TODO: Handle exceptions properly

    try {
      JsonNode node = objectMapper.readTree(data);

      if (node.getNodeType() != JsonNodeType.ARRAY) {
        System.out.println("Maleformed incoming message");
        System.out.println(data);
      }

      for (JsonNode message : node) {
        JsonNode dd = objectMapper.readTree(message.asText());
        if (dd.getNodeType() != JsonNodeType.OBJECT) {
          System.out.println(dd.getNodeType());
          System.out.println("Maleformed incoming message");
          System.out.println(dd);
          continue;
        }
        messages.put(dd.get("queue").asText(), dd.get("message").asText());
      }
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }

    return new MessageFrame(sessionId, messages);
  }

  public static class ConnectionFrame extends InboundFrame {
    public ConnectionFrame(String sessionId) {
      this.sessionId = sessionId;
    }
  }

  public static class CloseFrame extends InboundFrame {
    public CloseFrame(String sessionId) {
      this.sessionId = sessionId;
    }
  }

  public static class MessageFrame extends InboundFrame {
    public Map<String, String> messages;

    public MessageFrame(String sessionId, Map<String, String> messages) {
      this.sessionId = sessionId;
      this.messages = messages;
    }
  }
}
