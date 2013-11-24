package chatterbird.server.transport;


import chatterbird.server.ConnectionInfo;
import chatterbird.server.MessageConverter;
import chatterbird.server.Router;
import chatterbird.server.engine.Engine;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Values.CLOSE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

@Sharable
@Component
public class InternalTransport extends ChannelInboundHandlerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(InternalTransport.class);

  @Autowired
  private Engine engine;
  @Autowired
  private MessageConverter messageConverter;

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    FullHttpResponse response;
    DefaultFullHttpRequest request = (DefaultFullHttpRequest) msg;
    ConnectionInfo info = ctx.channel().attr(Router.STATE).get();

    List<ObjectNode> messages = messageConverter.convert(request.content().toString(CharsetUtil.UTF_8));

    if (messages != null) {
      for (ObjectNode message : messages) {
        engine.fireEvent(message.get("handler").asText(), message.get("event").asText(), message.get("data"));
      }

      response = new DefaultFullHttpResponse(info.httpVersion, OK);
    } else {
      response = new DefaultFullHttpResponse(info.httpVersion, BAD_REQUEST);
    }

    response.headers().set(CONNECTION, CLOSE);
    response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

    ctx.writeAndFlush(response);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    logger.error("Error processing internal message", cause);

    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, INTERNAL_SERVER_ERROR);
    response.headers().set(CONNECTION, CLOSE);
    response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

    ctx.writeAndFlush(response);
  }
}
