package chatterbird.server;


import chatterbird.ApplicationConfig;
import chatterbird.server.engine.Engine;
import chatterbird.server.frame.InboundFrame;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static chatterbird.server.engine.Engine.Events;

@Sharable
@Component
public class SessionManager extends SimpleChannelInboundHandler<InboundFrame> {
  private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
  @Autowired
  @Qualifier("workerGroup")
  private NioEventLoopGroup workerGroup;
  @Autowired
  private ApplicationConfig applicationConfig;
  @Autowired
  private Engine engine;
  @Autowired
  private MessageConverter messageConverter;
  private ConcurrentHashMap<String, Session> sessions;

  @PostConstruct
  public void init() {
    sessions = new ConcurrentHashMap<String, Session>();

    workerGroup.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        SessionManager.this.timeout();
      }
    }, applicationConfig.sessionTimeout, applicationConfig.sessionTimeout, TimeUnit.SECONDS);
  }

  public int getSessionsCount() {
    return sessions.size();
  }

  public void sendMessage(String handler, String sessionId, String event, JsonNode data) {
    Session session = sessions.get(sessionId);
    ObjectNode msg = messageConverter.convert(handler, event, data);

    if (session != null) {
      session.sendMessage(msg);
    } else {
      //TODO: this is temporary hack to prevent connect|disconnect order problem
      engine.fireEvent(sessionId, handler, Events.DISCONNECT);
    }
  }

  public void sendMessageConnected(String handler, String event, JsonNode data) {
    ObjectNode msg = messageConverter.convert(handler, event, data);

    for (Session session : sessions.values()) {
      if (session.handlers.contains(handler)) {
        session.sendMessage(msg);
      }
    }
  }

  private Session getOrCreateSession(String sessionId) {
    Session session = sessions.get(sessionId);
    if (session == null) {
      Session newSession = new Session(sessionId, engine, applicationConfig.sessionHeartbeat);
      session = sessions.putIfAbsent(sessionId, newSession);
      if (session == null) {
        session = newSession;
      }
    }

    return session;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, InboundFrame msg) throws Exception {
    if (msg instanceof InboundFrame.ConnectionFrame) {
      logger.debug("Register session {}", msg.sessionId);
      Session session = getOrCreateSession(msg.sessionId);
      session.register(ctx.channel());
    }
    if (msg instanceof InboundFrame.CloseFrame) {
      logger.debug("UnRegister session {}", msg.sessionId);
      Session session = sessions.get(msg.sessionId);

      if (session != null) {
        session.unregister(ctx.channel());
      }
    }
    if (msg instanceof InboundFrame.MessageFrame) {
      InboundFrame.MessageFrame message = (InboundFrame.MessageFrame) msg;
      Session session = sessions.get(msg.sessionId);

      if (session != null) {
        session.receiveMessages(messageConverter.convert(message.data));
      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
  }

  private void timeout() {
    Iterator<Map.Entry<String, Session>> iterator = sessions.entrySet().iterator();

    while (iterator.hasNext()) {
      Map.Entry<String, Session> entry = iterator.next();
      Session session = entry.getValue();

      if (session.channel.getStamp() == SessionState.CLOSING.getValue() && session.almostDeleted.compareAndSet(false, true)) {
        continue;
      }

      if (session.channel.compareAndSet(null, null, SessionState.CLOSING.getValue(), SessionState.CLOSED.getValue())) {
        logger.debug("Remove session {}", entry.getKey());
        iterator.remove();
        session.remove();
      }
    }
  }
}
