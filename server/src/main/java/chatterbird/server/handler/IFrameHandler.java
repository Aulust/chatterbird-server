package chatterbird.server.handler;


import chatterbird.server.Utils;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.CharsetUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

@Component
public class IFrameHandler {
  @Autowired
  private Environment env;

  private String content;

  @PostConstruct
  public void generateContent() {
    content = "<!DOCTYPE html>\n" +
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
  }

  public void handle(FullHttpRequest request, ChannelHandlerContext ctx) {
    FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(), OK,
        Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
    response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");
    response.headers().set(HttpHeaders.Names.CACHE_CONTROL, "max-age=31536000, public");
    response.headers().remove(HttpHeaders.Names.SET_COOKIE);

    ctx.writeAndFlush(response);
  }
}
