package chatterbird.server;


import chatterbird.server.engine.Engine;
import chatterbird.server.frame.InboundFrame;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Sharable
@Component
public class SessionManager extends SimpleChannelInboundHandler<InboundFrame> {
  private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
  @Autowired
  @Qualifier("timer")
  private Timer timer;
  @Autowired
  private Engine engine;
  @Autowired
  @Qualifier("objectMapper")
  private ObjectMapper objectMapper;
  //TODO make this private
  public ConcurrentHashMap<String, Session> sessions;

  public SessionManager() {
    sessions = new ConcurrentHashMap<String, Session>();
    timer = new HashedWheelTimer();

    heartbeatTimeout();
    sessionTimeout();
  }

  public void sendMessage(String name, String sessionId, String message) {
    Session session = sessions.get(sessionId);

    if (session != null) {
      try {
        session.sendMessage(objectMapper.writeValueAsString(ImmutableMap.<String, String>of("queue", name, "message", message)));
      } catch (JsonProcessingException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
    }
  }

  public void sendMessageConnected(String name, String message) {
    for (Session session : sessions.values()) {
      if (session.handlers.contains(name)) {
        try {
          session.sendMessage(objectMapper.writeValueAsString(ImmutableMap.<String, String>of("queue", name, "message", message)));
        } catch (JsonProcessingException e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
      }
    }
  }

  private Session getOrCreateSession(String sessionId) {
    Session session = sessions.get(sessionId);
    if (session == null) {
      Session newSession = new Session(sessionId, engine);
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
        session.receiveMessages(message.messages);
      }
      logger.debug("Got message {} from session {}", message.messages, message.sessionId);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
  }

  private void heartbeatTimeout() {
    Timeout heartbeatTimeout = timer.newTimeout(new TimerTask() {
      @Override
      public void run(Timeout timeout) throws Exception {
        if (timeout.isCancelled()) {
          return;
        }
        SessionManager.this.heartbeat();
      }
    }, 30, TimeUnit.SECONDS);
  }

  private void sessionTimeout() {
    Timeout sessionTimeout = timer.newTimeout(new TimerTask() {
      @Override
      public void run(Timeout timeout) throws Exception {
        if (timeout.isCancelled()) {
          return;
        }
        SessionManager.this.timeout();
      }
    }, 5, TimeUnit.SECONDS);
  }

  private void heartbeat() {
    for (Session session : sessions.values()) {
      session.heartbeat();
    }
    heartbeatTimeout();
  }

  private void timeout() {
    Iterator<Map.Entry<String, Session>> iterator = sessions.entrySet().iterator();

    while (iterator.hasNext()) {
      Map.Entry<String, Session> entry = iterator.next();

      if (entry.getValue().channel.compareAndSet(null, null, SessionState.CLOSING.getValue(), SessionState.CLOSED.getValue())) {
        logger.debug("Remove session {}", entry.getKey());
        iterator.remove();
        entry.getValue().remove();
      }
    }

    sessionTimeout();
  }
}
