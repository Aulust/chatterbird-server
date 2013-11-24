package chatterbird.server.frame;


public abstract class InboundFrame {
  public String sessionId;

  public static ConnectionFrame connectionFrame(String sessionId) {
    return new ConnectionFrame(sessionId);
  }

  public static CloseFrame closeFrame(String sessionId) {
    return new CloseFrame(sessionId);
  }

  public static MessageFrame messageFrame(String sessionId, String data) {
    return new MessageFrame(sessionId, data);
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
    public String data;

    public MessageFrame(String sessionId, String data) {
      this.sessionId = sessionId;
      this.data = data;
    }
  }
}
