package chatterbird.server.transport;

import chatterbird.server.frame.OutboundFrame;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.HttpObject;
import org.springframework.stereotype.Component;

import java.util.List;

@Sharable
@Component
public class StubTransport extends MessageToMessageCodec<HttpObject, OutboundFrame>  {

    @Override
    protected void encode(ChannelHandlerContext ctx, OutboundFrame msg, List<Object> out) throws Exception {
        System.out.println("Stub encode");
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out) throws Exception {
        System.out.println("Stub decode");
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
