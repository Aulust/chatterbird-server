package chatterbird.server.transport;


import chatterbird.server.ConnectionInfo;
import chatterbird.server.Router;
import chatterbird.server.Utils;
import chatterbird.server.frame.InboundFrame;
import chatterbird.server.frame.OutboundFrame;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

@Sharable
@Component
public class XhrSendTransport extends MessageToMessageCodec<HttpMessage, OutboundFrame> {
  @Override
  protected void encode(ChannelHandlerContext ctx, OutboundFrame msg, List<Object> out) throws Exception {
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, HttpMessage msg, List<Object> out) throws Exception {
    DefaultFullHttpRequest request = (DefaultFullHttpRequest) msg;

    ConnectionInfo info = ctx.channel().attr(Router.STATE).get();
    out.add(InboundFrame.messageFrame(info.sessionId, request.content().toString(CharsetUtil.UTF_8)));

    FullHttpResponse response = new DefaultFullHttpResponse(info.httpVersion, OK);
    response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
    response.headers().set(CONTENT_TYPE, "application/javascript; charset=UTF-8");
    Utils.enrichHeaders(response.headers(), info);
    ctx.writeAndFlush(response);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    //TODO: Handle exception properly
    cause.printStackTrace();
  }
}
