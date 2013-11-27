package examples;

import chatterbird.AMQPConfig;
import chatterbird.ApplicationConfig;
import io.netty.bootstrap.ServerBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractApplicationContext;

import java.net.InetSocketAddress;

@Configuration
@ComponentScan({"examples"})
public class Application {
  private static final Logger logger = LoggerFactory.getLogger(Application.class);

  public static void main(String[] args) {
    logger.info("Starting application context");
    AbstractApplicationContext ctx = new AnnotationConfigApplicationContext(ApplicationConfig.class, AMQPConfig.class, Application.class);
    ctx.registerShutdownHook();

    ServerBootstrap bootstrap = ctx.getBean(ServerBootstrap.class);
    InetSocketAddress tcpPort = ctx.getBean(InetSocketAddress.class);

    logger.info("Starting server at {}", tcpPort);
    try {
      bootstrap.bind(tcpPort).sync().channel().closeFuture().sync().channel().close();
    } catch (InterruptedException e) {
      logger.error("Error while server operation", e);
    }
    logger.info("Server stopped");
  }
}
