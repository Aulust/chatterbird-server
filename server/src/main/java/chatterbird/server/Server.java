package chatterbird.server;

import chatterbird.server.frame.InboundFrame;
import chatterbird.server.frame.OutboundFrame;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;

@Component
public class Server {
  private static final Logger logger = LoggerFactory.getLogger(Server.class);

	@Autowired
	@Qualifier("serverBootstrap")
	private ServerBootstrap bootstrap;

	@Autowired
	@Qualifier("tcpSocketAddress")
	private InetSocketAddress tcpPort;

  @Autowired
  @Qualifier("objectMapper")
  private ObjectMapper objectMapper;

	@PostConstruct
	public void start() throws Exception {
    InboundFrame.setObjectMapper(objectMapper);
    OutboundFrame.setObjectMapper(objectMapper);

    new Thread(new Runnable() {
        public void run() {
          try {
            logger.info("Starting chatterbird.server at {}", tcpPort);
            bootstrap.bind(tcpPort).sync().channel().closeFuture().sync().channel().close();
          } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
          }
        }
    }).start();
	}
}
