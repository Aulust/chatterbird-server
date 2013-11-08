package chatterbird.server;


import io.netty.handler.codec.http.HttpHeaders;
import org.apache.commons.lang3.StringUtils;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_METHODS;
import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static io.netty.handler.codec.http.HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN;

public class Utils {
  public static void enrichHeaders(HttpHeaders headers, ConnectionInfo info) {
    headers.set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    headers.set(ACCESS_CONTROL_ALLOW_METHODS, "OPTIONS, GET, POST");
    headers.set(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    headers.set(ACCESS_CONTROL_ALLOW_ORIGIN, StringUtils.isNotEmpty(info.origin) ? info.origin : "*");
  }
}
