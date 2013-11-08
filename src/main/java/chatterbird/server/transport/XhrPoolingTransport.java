package chatterbird.server.transport;


import chatterbird.server.Router;
import chatterbird.server.ConnectionInfo;
import chatterbird.server.Utils;
import chatterbird.server.frame.InboundFrame;
import chatterbird.server.frame.OutboundFrame;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.*;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

@Sharable
@Component
public class XhrPoolingTransport extends MessageToMessageCodec<HttpMessage, OutboundFrame> {
  @Override
  protected void encode(ChannelHandlerContext ctx, OutboundFrame msg, List<Object> out) throws Exception {
    ConnectionInfo info = ctx.channel().attr(Router.STATE).get();

    FullHttpResponse response = new DefaultFullHttpResponse(info.httpVersion, OK, msg.getData());
    response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
    response.headers().set(CONTENT_TYPE, "application/javascript; charset=UTF-8");
    Utils.enrichHeaders(response.headers(), info);
    out.add(response);

    ctx.fireChannelRead(InboundFrame.closeFrame(info.sessionId));
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, HttpMessage msg, List<Object> out) throws Exception {
    ConnectionInfo info = ctx.channel().attr(Router.STATE).get();
    out.add(new InboundFrame.ConnectionFrame(info.sessionId));
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    ConnectionInfo info = ctx.channel().attr(Router.STATE).get();
    ctx.fireChannelRead(InboundFrame.closeFrame(info.sessionId));
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
  }
}
