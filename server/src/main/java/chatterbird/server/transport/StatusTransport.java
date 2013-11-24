package chatterbird.server.transport;


import chatterbird.server.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

@Sharable
@Component
public class StatusTransport extends ChannelInboundHandlerAdapter {
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private SessionManager sessionManager;
  @Autowired
  private ThreadPoolExecutor threadPoolExecutor;

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    DefaultFullHttpRequest request = (DefaultFullHttpRequest) msg;

    ObjectNode result = objectMapper.createObjectNode();
    result.set("sessions", IntNode.valueOf(sessionManager.getSessionsCount()));
    result.set("remainingCapacity", IntNode.valueOf(threadPoolExecutor.getQueue().remainingCapacity()));
    result.set("threadsCount", IntNode.valueOf(Thread.activeCount()));

    FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(), OK, Unpooled.copiedBuffer(result.toString(), CharsetUtil.UTF_8));
    response.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
    response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

    ctx.writeAndFlush(response);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

  }
}
