package chatterbird.server.transport;

import chatterbird.server.frame.OutboundFrame;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.HttpObject;
import org.springframework.stereotype.Component;

import java.util.List;

@Sharable
@Component
public class StubTransport extends ChannelInboundHandlerAdapter {
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

  }
}
