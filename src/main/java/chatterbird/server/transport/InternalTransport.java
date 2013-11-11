package chatterbird.server.transport;


import chatterbird.server.ConnectionInfo;
import chatterbird.server.Router;
import chatterbird.server.Utils;
import chatterbird.server.engine.Engine;
import chatterbird.server.frame.InboundFrame;
import chatterbird.server.frame.OutboundFrame;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.util.CharsetUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Values.CLOSE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

@Sharable
@Component
public class InternalTransport extends MessageToMessageCodec<HttpMessage, OutboundFrame> {
  @Autowired
  private Engine engine;

  @Override
  protected void encode(ChannelHandlerContext ctx, OutboundFrame msg, List<Object> out) throws Exception {
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, HttpMessage msg, List<Object> out) throws Exception {
    FullHttpResponse response;
    DefaultFullHttpRequest request = (DefaultFullHttpRequest) msg;
    ConnectionInfo info = ctx.channel().attr(Router.STATE).get();

    if (engine.isHandlerExists(info.sessionId)) {
      engine.internalMessageEvent(info.sessionId, request.content().toString(CharsetUtil.UTF_8));
      response = new DefaultFullHttpResponse(info.httpVersion, NO_CONTENT);
    } else {
      response = new DefaultFullHttpResponse(info.httpVersion, NOT_FOUND);
    }

    response.headers().set(CONNECTION, CLOSE);
    ctx.writeAndFlush(response);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    //TODO: Handle exception properly
    cause.printStackTrace();
  }
}
