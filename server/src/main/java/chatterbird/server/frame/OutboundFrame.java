package chatterbird.server.frame;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

import java.util.List;

public abstract class OutboundFrame {
  private static final OpenFrame OPEN_FRAME_OBJ = new OpenFrame();
  private static final HeartbeatFrame HEARTBEAT_FRAME_OBJ = new HeartbeatFrame();
  protected ByteBuf data;

  public static OpenFrame openFrame() {
    return OPEN_FRAME_OBJ;
  }

  public static HeartbeatFrame heartbeatFrame() {
    return HEARTBEAT_FRAME_OBJ;
  }

  public static CloseFrame closeFrame(int status, String reason) {
    return new CloseFrame(status, reason);
  }

  public static MessageFrame messageFrame(List<JsonNode> messages) {
    return new MessageFrame(messages);
  }

  public ByteBuf getData() {
    return data;
  }

  public static class OpenFrame extends OutboundFrame {
    @Override
    public ByteBuf getData() {
      return Unpooled.copiedBuffer("o\n", CharsetUtil.UTF_8);
    }
  }

  public static class HeartbeatFrame extends OutboundFrame {
    @Override
    public ByteBuf getData() {
      return Unpooled.copiedBuffer("h\n", CharsetUtil.UTF_8);
    }
  }

  public static class CloseFrame extends OutboundFrame {
    private CloseFrame(int status, String reason) {
      data = Unpooled.copiedBuffer("c[" + status + ",\"" + reason + "\"]\n", CharsetUtil.UTF_8);
    }
  }

  //TODO This fish smells bad
  public static class MessageFrame extends OutboundFrame {
    private MessageFrame(List<JsonNode> messages) {
      data = Unpooled.buffer();
      data.writeByte('a');
      data.writeByte('[');
      for (int i = 0; i < messages.size(); i++) {
        data.writeBytes(Unpooled.copiedBuffer(messages.get(i).toString(), CharsetUtil.UTF_8));
        if (i < messages.size() - 1) {
          data.writeByte(',');
        }
      }

      data.writeByte(']');
      data.writeByte('\n');
    }
  }
}
