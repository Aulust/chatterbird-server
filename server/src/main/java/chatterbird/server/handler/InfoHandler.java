package chatterbird.server.handler;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.CharsetUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_METHODS;
import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.ORIGIN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

@Component
public class InfoHandler {
  @Autowired
  private Environment env;
  @Autowired
  private ObjectMapper objectMapper;

  private String content;

  @PostConstruct
  public void generateContent() throws JsonProcessingException {
    content = objectMapper.writeValueAsString(ImmutableMap.of("websocket", false, "cookie_needed", false));
  }

  public void handle(FullHttpRequest request, ChannelHandlerContext ctx) {
    String origin = request.headers().get(ORIGIN);

    FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(), OK,
        Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
    response.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
    response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
    response.headers().set(ACCESS_CONTROL_ALLOW_METHODS, "OPTIONS, GET, POST");
    response.headers().set(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    response.headers().set(ACCESS_CONTROL_ALLOW_ORIGIN, StringUtils.isNotEmpty(origin) ? origin : "*");

    ctx.writeAndFlush(response);
  }
}
