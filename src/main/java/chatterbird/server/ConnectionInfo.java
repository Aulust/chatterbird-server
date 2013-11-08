package chatterbird.server;


import io.netty.handler.codec.http.HttpVersion;

public class ConnectionInfo {
  public String sessionId;
  public HttpVersion httpVersion;
  public String origin;
  public String handler;
}
