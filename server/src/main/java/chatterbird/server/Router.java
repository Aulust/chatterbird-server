package chatterbird.server;


import chatterbird.server.frame.InboundFrame;
import chatterbird.server.handler.IFrameHandler;
import chatterbird.server.handler.InfoHandler;
import chatterbird.server.handler.NotFoundHandler;
import chatterbird.server.handler.OptionsHandler;
import chatterbird.server.transport.InternalTransport;
import chatterbird.server.transport.StatusTransport;
import chatterbird.server.transport.XhrPoolingTransport;
import chatterbird.server.transport.XhrSendTransport;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static io.netty.handler.codec.http.HttpHeaders.Names.ORIGIN;

@Component
@ChannelHandler.Sharable
public class Router extends ChannelInboundHandlerAdapter {
  public static final AttributeKey<ConnectionInfo> STATE = new AttributeKey<ConnectionInfo>("connection.info");
  private static final Logger logger = LoggerFactory.getLogger(Router.class);

  @Autowired
  private IFrameHandler iFrameHandler;
  @Autowired
  private InfoHandler infoHandler;
  @Autowired
  private OptionsHandler optionsHandler;
  @Autowired
  private NotFoundHandler notFoundHandler;
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
    //TODO
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
      notFoundHandler.handle(request, ctx);
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
      optionsHandler.handle(request, ctx);
      return;
    } else if (HttpMethod.GET.equals(httpMethod)) {
      if ("info".equals(method)) {
        infoHandler.handle(request, ctx);
        return;
      }
      if ("iframe.html".equals(method)) {
        iFrameHandler.handle(request, ctx);
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

    notFoundHandler.handle(request, ctx);
  }
}
