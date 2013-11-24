package chatterbird.server;


import chatterbird.server.frame.InboundFrame;
import chatterbird.server.transport.InternalTransport;
import chatterbird.server.transport.StatusTransport;
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
import org.springframework.core.env.Environment;
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
  Environment env;
  @Autowired
  private XhrPoolingTransport xhrPoolingTransport;
  @Autowired
  private XhrSendTransport xhrSendTransport;
  @Autowired
  private InternalTransport internalTransport;
  @Autowired
  private StatusTransport statusTransport;

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
      if ("iframe.html".equals(method)) {
          String content = "<!DOCTYPE html>\n" +
          "<html>\n" +
          "<head>\n" +
          "  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\" />\n" +
          "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
          "  <script>\n" +
          "    document.domain = document.domain;\n" +
          "    _sockjs_onload = function(){SockJS.bootstrap_iframe();};\n" +
          "  </script>\n" +
          "  <script src=\"" + env.getProperty("js.url") + "\"></script>\n" +
          "</head>\n" +
          "<body>\n" +
          "  <h2>Don't panic!</h2>\n" +
          "  <p>This is a SockJS hidden iframe. It's used for cross domain magic.</p>\n" +
          "</body>\n" +
          "</html>";

        FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(), OK, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");
        response.headers().set(HttpHeaders.Names.CACHE_CONTROL, "max-age=31536000, public");
        response.headers().remove(HttpHeaders.Names.SET_COOKIE);
        Utils.enrichHeaders(response.headers(), info);
        ctx.writeAndFlush(response);

        return;
      }
      if ("status".equals(method)) {
        ctx.channel().pipeline().replace("transport", "transport", statusTransport);
        ctx.fireChannelRead(msg);

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
