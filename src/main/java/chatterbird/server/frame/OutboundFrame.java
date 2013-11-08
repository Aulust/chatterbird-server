package chatterbird.server.frame;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

import java.util.List;

public abstract class OutboundFrame {
  //TODO Get rid of this static shit
  private static ObjectMapper objectMapper;

    private static final OpenFrame OPEN_FRAME_OBJ = new OpenFrame();
    private static final HeartbeatFrame HEARTBEAT_FRAME_OBJ = new HeartbeatFrame();
    /*private static final ChannelBuffer OPEN_FRAME = ChannelBuffers.copiedBuffer("o", CharsetUtil.UTF_8);
    private static final ChannelBuffer OPEN_FRAME_NL = ChannelBuffers.copiedBuffer("o\n", CharsetUtil.UTF_8);
    private static final HeartbeatOutboundFrame HEARTBEAT_FRAME_OBJ = new HeartbeatOutboundFrame();
    private static final ChannelBuffer HEARTBEAT_FRAME = ChannelBuffers.copiedBuffer("h", CharsetUtil.UTF_8);
    private static final ChannelBuffer HEARTBEAT_FRAME_NL = ChannelBuffers.copiedBuffer("h\n", CharsetUtil.UTF_8);
    private static final PreludeFrame PRELUDE_FRAME_OBJ = new PreludeFrame();
    private static final ChannelBuffer PRELUDE_FRAME = generatePreludeFrame('h', 2048, false);
    private static final ChannelBuffer PRELUDE_FRAME_NL = generatePreludeFrame('h', 2048, true);
    private static final ChannelBuffer NEW_LINE = ChannelBuffers.copiedBuffer("\n", CharsetUtil.UTF_8);*/

  public static void setObjectMapper(ObjectMapper objectMapper) {;
    OutboundFrame.objectMapper = objectMapper;
  }

    protected ByteBuf data;

    public ByteBuf getData() {
        return data;
    }

    public static OpenFrame openFrame() {
        return OPEN_FRAME_OBJ;
    }

    public static HeartbeatFrame heartbeatFrame() {
        return HEARTBEAT_FRAME_OBJ;
    }

    /*public static CloseFrame closeFrame(int status, String reason) {
        return new CloseFrame(status, reason);
    }

    public static HeartbeatOutboundFrame heartbeatFrame() {
        return HEARTBEAT_FRAME_OBJ;
    }

    public static PreludeFrame preludeFrame() {
        return PRELUDE_FRAME_OBJ;
    }*/

    public static MessageFrame messageFrame(List<String> messages) {
        return new MessageFrame(messages);
    }

    /*public static ChannelBuffer encode(OutboundFrame frame, boolean appendNewline) {
        if (frame instanceof OpenOutboundFrame) {
            return appendNewline ? OPEN_FRAME_NL : OPEN_FRAME;
        } else if (frame instanceof HeartbeatOutboundFrame) {
            return appendNewline ? HEARTBEAT_FRAME_NL : HEARTBEAT_FRAME;
        } else if (frame instanceof PreludeFrame) {
            return appendNewline ? PRELUDE_FRAME_NL : PRELUDE_FRAME;
        } else if (frame instanceof MessageFrame || frame instanceof CloseFrame) {
            return appendNewline ? ChannelBuffers.wrappedBuffer(frame.getData(), NEW_LINE) : frame.getData();
        } else {
            throw new IllegalArgumentException("Unknown frame type passed: " + frame.getClass().getSimpleName());
        }
    }

    private static ChannelBuffer generatePreludeFrame(char c, int num, boolean appendNewline) {
        ChannelBuffer cb = ChannelBuffers.buffer(num + 1);
        for (int i = 0; i < num; i++) {
              cb.writeByte(c);
        }
        if (appendNewline)
            cb.writeByte('\n');
        return cb;
    }

    public static String escapeCharacters(char[] value) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < value.length; i++) {
            char ch = value[i];
            if ((ch >= '\u0000' && ch <= '\u001F') ||
                    (ch >= '\uD800' && ch <= '\uDFFF') ||
                    (ch >= '\u200C' && ch <= '\u200F') ||
                    (ch >= '\u2028' && ch <= '\u202F') ||
                    (ch >= '\u2060' && ch <= '\u206F') ||
                    (ch >= '\uFFF0' && ch <= '\uFFFF')) {
                String ss = Integer.toHexString(ch);
                buffer.append('\\');
                buffer.append('u');
                for (int k = 0; k < 4 - ss.length(); k++) {
                    buffer.append('0');
                }
                buffer.append(ss.toLowerCase());
            } else {
                buffer.append(ch);
            }
        }
        return buffer.toString();
    }
    

    public static void escapeJson(ChannelBuffer input, ChannelBuffer buffer) {
        for (int i = 0; i < input.readableBytes(); i++) {
            byte ch = input.getByte(i);
            switch(ch) {
                case '"': buffer.writeByte('\\'); buffer.writeByte('\"'); break;
                case '/': buffer.writeByte('\\'); buffer.writeByte('/'); break;
                case '\\': buffer.writeByte('\\'); buffer.writeByte('\\'); break;
                case '\b': buffer.writeByte('\\'); buffer.writeByte('b'); break;
                case '\f': buffer.writeByte('\\'); buffer.writeByte('f'); break;
                case '\n': buffer.writeByte('\\'); buffer.writeByte('n'); break;
                case '\r': buffer.writeByte('\\'); buffer.writeByte('r'); break;
                case '\t': buffer.writeByte('\\'); buffer.writeByte('t'); break;

                default:
                    // Reference: http://www.unicode.org/versions/Unicode5.1.0/
                    if ((ch >= '\u0000' && ch <= '\u001F') ||
                            (ch >= '\uD800' && ch <= '\uDFFF') ||
                            (ch >= '\u200C' && ch <= '\u200F') ||
                            (ch >= '\u2028' && ch <= '\u202F') ||
                            (ch >= '\u2060' && ch <= '\u206F') ||
                            (ch >= '\uFFF0' && ch <= '\uFFFF')) {
                        String ss = Integer.toHexString(ch);
                        buffer.writeByte('\\');
                        buffer.writeByte('u');
                        for (int k = 0; k < 4 - ss.length(); k++) {
                            buffer.writeByte('0');
                        }
                        buffer.writeBytes(ss.toLowerCase().getBytes());
                    } else {
                        buffer.writeByte(ch);
                    }
            }
        }
    }*/

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

    /*public static class CloseFrame extends OutboundFrame {
        private int status;
        private String reason;

        private CloseFrame(int status, String reason) {
            this.status = status;
            this.reason = reason;
            // FIXME: Must escape status and reason
            data = ChannelBuffers.copiedBuffer("c[" + status + ",\"" + reason + "\"]", CharsetUtil.UTF_8);
        }

        public int getStatus() {
            return status;
        }

        public String getReason() {
            return reason;
        }
    }*/

    //TODO This fish smells bad
    public static class MessageFrame extends OutboundFrame {
        private MessageFrame(List<String> messages) {
            data = Unpooled.buffer();
            data.writeByte('a');
            data.writeByte('[');
            for (int i = 0; i < messages.size(); i++) {
                String message = messages.get(i);
                data.writeBytes(Unpooled.copiedBuffer(message, CharsetUtil.UTF_8));
                if (i < messages.size() - 1) {
                    data.writeByte(',');
                }
            }

            data.writeByte(']');
          data.writeByte('\n');
        }
    }

    /*public static class HeartbeatOutboundFrame extends OutboundFrame {
        @Override
        public ChannelBuffer getData() {
            return HEARTBEAT_FRAME;
        }
    }

    public static class PreludeFrame extends OutboundFrame {
        @Override
        public ChannelBuffer getData() {
            return PRELUDE_FRAME;
        }
    }*/
}
