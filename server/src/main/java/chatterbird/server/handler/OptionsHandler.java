package chatterbird.server.handler;


import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_METHODS;
import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN;
import static io.netty.handler.codec.http.HttpHeaders.Names.ORIGIN;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;

@Component
public class OptionsHandler {
  public void handle(FullHttpRequest request, ChannelHandlerContext ctx) {
    String origin = request.headers().get(ORIGIN);

    FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(), NO_CONTENT);
    response.headers().set(HttpHeaders.Names.CACHE_CONTROL, "max-age=31536000, public");
    response.headers().set("Access-Control-Max-Age", "31536000");
    response.headers().set("Access-Control-Allow-Headers", "Content-Type");
    response.headers().set(ACCESS_CONTROL_ALLOW_METHODS, "OPTIONS, GET, POST");
    response.headers().set(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    response.headers().set(ACCESS_CONTROL_ALLOW_ORIGIN, StringUtils.isNotEmpty(origin) ? origin : "*");

    ctx.writeAndFlush(response);

  }
}
