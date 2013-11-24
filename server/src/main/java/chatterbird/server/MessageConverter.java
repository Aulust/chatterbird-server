package chatterbird.server;


import chatterbird.server.engine.Engine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Component
public class MessageConverter {
  private static final Logger logger = LoggerFactory.getLogger(MessageConverter.class);
  @Autowired
  private Engine engine;
  @Autowired
  @Qualifier("objectMapper")
  private ObjectMapper objectMapper;

  public ObjectNode convert(String name, String event, JsonNode data) {
    ObjectNode result = objectMapper.createObjectNode();
    result.set("handler", TextNode.valueOf(name));
    result.set("event", TextNode.valueOf(event));
    result.set("data", data);

    return result;
  }

  public List<ObjectNode> convert(String data) {
    JsonNode node;
    List<ObjectNode> messages = new ArrayList<ObjectNode>();

    try {
      node = objectMapper.readTree(data);
    } catch (IOException e) {
      logger.error("Can't parse request data", e);
      return null;
    }

    if (node.getNodeType() != JsonNodeType.ARRAY) {
      logger.error("Request data is not array: {}", node);
      return null;
    }

    for (JsonNode message : node) {
      if (message.getNodeType() == JsonNodeType.STRING) {
        try {
          message = objectMapper.readTree(message.asText());
        } catch (IOException e) {
          logger.error("Message has invalid format: {}", message);
          return null;
        }
      }

      if (message.getNodeType() != JsonNodeType.OBJECT || message.get("handler") == null || message.get("event") == null) {
        logger.error("Message has invalid format: {}", message);
        return null;
      }
      if (!engine.isHandlerExists(message.get("handler").asText())) {
        logger.error("Handler does not exist: {}", message.get("handler"));
        return null;
      }

      messages.add((ObjectNode) message);
    }

    return messages;
  }
}
