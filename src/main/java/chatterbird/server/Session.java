package chatterbird.server;


import chatterbird.server.engine.Engine;
import chatterbird.server.frame.OutboundFrame;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicStampedReference;

/*
 * Possible states:
 * 1. Newly created (null, CONNECTING)
 * 2. Channel bound to session (channel, OPEN)
 * 3. Channel unbound (null, CLOSING)
 * 4. Closed (null, CLOSED)
 */
public class Session {
  public AtomicStampedReference<Channel> channel;
  public CopyOnWriteArrayList<String> handlers;
  private LinkedBlockingQueue<String> messages;
  public final String sessionId;
  private final Engine engine;

  public Session(String sessionId, Engine engine) {
    this.sessionId = sessionId;
    this.engine = engine;

    channel = new AtomicStampedReference<Channel>(null, SessionState.CONNECTING.getValue());
    messages = new LinkedBlockingQueue<String>(100);
    handlers = new CopyOnWriteArrayList<String>();
  }

  public void register(Channel channel) {
    if (this.channel.compareAndSet(null, channel, SessionState.CLOSING.getValue(), SessionState.OPEN.getValue())) {
      //tryFlush();
      return;
    }

    if (this.channel.compareAndSet(null, channel, SessionState.CONNECTING.getValue(), SessionState.OPEN.getValue())) {
      this.channel.getReference().writeAndFlush(OutboundFrame.openFrame());
      return;
    }

    // Some other channel trying to use session
  }

  public void unregister(Channel channel) {
    if (this.channel.compareAndSet(channel, null, SessionState.OPEN.getValue(), SessionState.CLOSING.getValue())) {
      //Bla! :)
    }
    /*if (this.channel.get().hashCode() == channel.hashCode()) {
    }*/
  }

  public void receiveMessages(Map<String, String> messages) {
    for (Map.Entry<String, String> message : messages.entrySet()) {
      if (handlers.contains(message.getKey())) {
        engine.messageEvent(message.getKey(), sessionId, message.getValue());
      } else {
        handlers.add(message.getKey());
        engine.newClientEvent(message.getKey(), sessionId);
      }
    }
  }

  public boolean sendMessage(String message) {
    Channel currentChannel = channel.getReference();
    if (messages.offer(message) && currentChannel != null) {
      currentChannel.eventLoop().execute(new Runnable() {
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
    List<String> preparedMessages = new ArrayList<String>();
    Channel currentChannel = channel.getReference();

    if (currentChannel != null) {
      messages.drainTo(preparedMessages);
      currentChannel.writeAndFlush(OutboundFrame.messageFrame(preparedMessages));
    }
  }

  public void remove() {
    handlers.clear();
    messages.clear();
  }

  public void heartbeat() {
    if (this.channel.getStamp() == SessionState.OPEN.getValue()) {
      this.channel.getReference().writeAndFlush(OutboundFrame.heartbeatFrame());
    }
  }
}
