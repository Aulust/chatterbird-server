package chatterbird.server;


import chatterbird.server.engine.Engine;
import chatterbird.server.engine.Engine.Events;
import chatterbird.server.frame.OutboundFrame;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicStampedReference;

/*
 * Possible states:
 * 1. Newly created (null, CONNECTING)
 * 2. Channel bound to session (channel, OPEN)
 * 3. Channel unbound (null, CLOSING)
 * 4. Closed (null, CLOSED)
 *
 * 1 -> 2
 * 2 <-> 3
 * 3 -> 4
 */
public class Session {
  public AtomicStampedReference<Channel> channel;
  public CopyOnWriteArrayList<String> handlers;
  private LinkedBlockingQueue<JsonNode> messages;
  private ScheduledFuture heartbeatTimeout;
  public final String sessionId;
  private final Engine engine;
  private final int sessionHeartbeat;
  public AtomicBoolean almostDeleted = new AtomicBoolean(false);

  public Session(String sessionId, Engine engine, int sesssionHeartbeat) {
    this.sessionId = sessionId;
    this.engine = engine;
    this.sessionHeartbeat = sesssionHeartbeat;

    channel = new AtomicStampedReference<Channel>(null, SessionState.CONNECTING.getValue());
    messages = new LinkedBlockingQueue<JsonNode>(100);
    handlers = new CopyOnWriteArrayList<String>();
  }

  public void register(Channel channel) {
    if (this.channel.compareAndSet(null, channel, SessionState.CLOSING.getValue(), SessionState.OPEN.getValue())) {
      this.almostDeleted.set(false);
      tryFlush();
      return;
    }

    if (this.channel.compareAndSet(null, channel, SessionState.CONNECTING.getValue(), SessionState.OPEN.getValue())) {
      this.heartbeatTimeout = startHeartbeatTimeout(channel);
      this.channel.getReference().writeAndFlush(OutboundFrame.openFrame());
      return;
    }

    channel.writeAndFlush(OutboundFrame.closeFrame(2010, "Another connection still open"));
  }

  public void unregister(Channel channel) {
    if (this.channel.compareAndSet(channel, null, SessionState.OPEN.getValue(), SessionState.CLOSING.getValue())) {
      this.heartbeatTimeout.cancel(false);
    }
  }

  //TODO: This is COMPLETELY broken. Delete could happen before connect for handler.
  // Need to rewrite using MultithreadEventExecutorGroup with guarantee for happens-before
  public void receiveMessages(List<ObjectNode> messages) {
    if (messages == null) {
      return;
    }

    for (ObjectNode message : messages) {
      String handler = message.get("handler").asText();
      String event = message.get("event").asText();
      JsonNode data = message.get("data");

      if (Events.CONNECT.equals(event) && handlers.addIfAbsent(handler)) {
        engine.fireEvent(sessionId, handler, event, data);
        continue;
      }

      if (!Events.isReservedEvent(event) && handlers.contains(handler)) {
        engine.fireEvent(sessionId, handler, event, data);
      }
    }
  }

  public boolean sendMessage(JsonNode message) {
    Channel currentChannel = channel.getReference();
    if (messages.offer(message) && currentChannel != null) {
      //TODO: only one scheduled task needed
      currentChannel.eventLoop().submit(new Runnable() {
        @Override
        public void run() {
          Session.this.tryFlush();
        }
      });
      return true;
    }

    return false;
  }

  private void tryFlush() {
    Channel currentChannel = channel.getReference();

    if (currentChannel != null && currentChannel.isWritable() && !messages.isEmpty()) {
      List<JsonNode> preparedMessages = new ArrayList<JsonNode>();
      messages.drainTo(preparedMessages);
      currentChannel.writeAndFlush(OutboundFrame.messageFrame(preparedMessages));
    }

    if (heartbeatTimeout != null) {
      heartbeatTimeout.cancel(false);
    }
    heartbeatTimeout = startHeartbeatTimeout(currentChannel);
  }

  public void remove() {
    for (String handler : handlers) {
      engine.fireEvent(sessionId, handler, Events.DISCONNECT);
    }

    handlers.clear();
    messages.clear();
  }

  private ScheduledFuture startHeartbeatTimeout(Channel channel) {
    return channel.eventLoop().schedule(new Runnable() {
      @Override
      public void run() {
        Session.this.heartbeat();
      }
    }, sessionHeartbeat, TimeUnit.SECONDS);
  }

  public void heartbeat() {
    if (this.channel.getStamp() == SessionState.OPEN.getValue() && this.channel.getReference().isWritable()) {
      this.channel.getReference().writeAndFlush(OutboundFrame.heartbeatFrame());
    }
    startHeartbeatTimeout(this.channel.getReference());
  }
}
