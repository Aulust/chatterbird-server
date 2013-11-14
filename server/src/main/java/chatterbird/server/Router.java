package chatterbird.server;


import chatterbird.server.frame.InboundFrame;
import chatterbird.server.transport.InternalTransport;
import chatterbird.server.transport.XhrPoolingTransport;
import chatterbird.server.transport.XhrSendTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;

@Component
@ChannelHandler.Sharable
public class Router extends ChannelInboundHandlerAdapter {
  public static final AttributeKey<ConnectionInfo> STATE = new AttributeKey<ConnectionInfo>("connection.info");
  private static final Logger logger = LoggerFactory.getLogger(Router.class);
  @Autowired
  @Qualifier("objectMapper")
  private ObjectMapper objectMapper;
  @Autowired
  private XhrPoolingTransport xhrPoolingTransport;
  @Autowired
  private XhrSendTransport xhrSendTransport;
  @Autowired
  private InternalTransport internalTransport;

  private ConnectionInfo getConnectionInfo(ChannelHandlerContext ctx) {
    ConnectionInfo info = ctx.channel().attr(STATE).get();

    if (info == null) {
      info = new ConnectionInfo();
      ctx.channel().attr(STATE).set(info);
    }

    return info;
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    FullHttpRequest request = (FullHttpRequest) msg;

    ConnectionInfo info = this.getConnectionInfo(ctx);
    info.httpVersion = request.getProtocolVersion();
    info.origin = request.headers().get(ORIGIN);

    String url = request.getUri();
    String[] params = url.split("/");

    if (params.length < 2) {
      notFound(request, ctx);
      return;
    }

    String sessionId = params[params.length - 2];
    String method = params[params.length - 1];
    HttpMethod httpMethod = request.getMethod();

    if (StringUtils.isNotEmpty(info.sessionId) && !StringUtils.equals(sessionId, info.sessionId)) {
      logger.debug("Channel session id changed from {} to {}, unregister previous session", info.sessionId, sessionId);
      ctx.fireChannelRead(new InboundFrame.CloseFrame(info.sessionId));
    }
    info.sessionId = sessionId;

    if (HttpMethod.OPTIONS.equals(httpMethod)) {
      FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(), NO_CONTENT);
      response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
      response.headers().set(HttpHeaders.Names.CACHE_CONTROL, "max-age=31536000, public");
      response.headers().set("Access-Control-Max-Age", "31536000");
      response.headers().set("Access-Control-Allow-Headers", "Content-Type");

      Utils.enrichHeaders(response.headers(), info);
      ctx.writeAndFlush(response);

      return;
    } else if (HttpMethod.GET.equals(httpMethod)) {
      if ("info".equals(method)) {
        Map<String, Boolean> resp = ImmutableMap.of("websocket", false, "cookie_needed", false);
        FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(), OK, Unpooled.copiedBuffer(objectMapper.writeValueAsString(resp), CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        Utils.enrichHeaders(response.headers(), info);
        ctx.writeAndFlush(response);

        return;
      }
    } else if (HttpMethod.POST.equals(httpMethod)) {
      if ("xhr".equals(method)) {
        if (!StringUtils.equals(info.handler, method)) {
          logger.debug("{} session was bound to xhr handler", info.sessionId);
          ctx.channel().pipeline().replace("transport", "transport", xhrPoolingTransport);
          info.handler = method;
        }

        ctx.fireChannelRead(msg);
        return;
          /*if(bla == 1) {
              FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer("o\n", CharsetUtil.UTF_8));
              response.headers().set(CONTENT_TYPE, "application/javascript; charset=UTF-8");
              response.headers().set(TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
              response.headers().set(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
              response.headers().set("Access-Control-Allow-Credentials", "true");
              response.headers().set("Access-Control-Allow-Methods", "OPTIONS, GET, POST");
              response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
              ctx.write(response);
              ctx.flush();

              /*HttpContent responseData = new DefaultLastHttpContent(Unpooled.copiedBuffer("o\n", CharsetUtil.UTF_8));
              ctx.write(responseData);
              ctx.flush();*/

              /*bla =2;
          } else {*/
/*                    HttpResponse response = new DefaultHttpResponse(HTTP_1_1, CONTINUE);
              response.headers().set(CONTENT_TYPE, "application/javascript; charset=UTF-8");
              response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
              response.headers().set(TRANSFER_ENCODING, "chunked");
              //response.headers().set("Access-Control-Allow-Methods", "OPTIONS, GET, POST");
              //response.headers().set("Access-Control-Allow-Credentials", "true");
              //response.headers().set(ACCESS_CONTROL_ALLOW_ORIGIN, "http://chatterbird.local");
              ctx.write(response);
              ctx.flush();*/
  //}
      } else if ("xhr_send".equals(method)) {
        if (!StringUtils.equals(info.handler, method)) {
          logger.debug("{} session was bound to xhr send handler", info.sessionId);
          ctx.channel().pipeline().replace("transport", "transport", xhrSendTransport);
          info.handler = method;
        }

        ctx.fireChannelRead(msg);
        return;
      } else if ("internal".equals(method)) {
        ctx.channel().pipeline().replace("transport", "transport", internalTransport);
        ctx.fireChannelRead(msg);
        return;
      }
    }

    notFound(request, ctx);
  }

  private void notFound(FullHttpRequest request, ChannelHandlerContext ctx) {
    FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(), NOT_FOUND);
    ctx.writeAndFlush(response);
  }
}
