package chatterbird;

import chatterbird.server.Router;
import chatterbird.server.SessionManager;
import chatterbird.server.transport.StubTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.net.InetSocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@Configuration
@ComponentScan({"chatterbird"})
@PropertySource("classpath:chatterbird.properties")
public class ApplicationConfig {
  @Value("${boss.thread.count}")
  private int bossCount;
  @Value("${worker.thread.count}")
  private int workerCount;
  @Value("${tcp.port}")
  private int tcpPort;
  @Value("${so.keepalive}")
  private boolean keepAlive;
  @Value("${so.backlog}")
  private int backlog;
  @Value("${tpe.corePoolSize}")
  private int corePoolSize;
  @Value("${tpe.maximumPoolSize}")
  private int maximumPoolSize;
  @Value("${tpe.keepAliveTime}")
  private int keepAliveTime;
  @Value("${tpe.queueSize}")
  private int queueSize;
  @Autowired
  private Router router;
  @Autowired
  private StubTransport stubTransport;
  @Autowired
  private SessionManager sessionManager;

  @Bean
  public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }

  @SuppressWarnings("unchecked")
  @Bean(name = "serverBootstrap")
  public ServerBootstrap bootstrap() {
    ServerBootstrap b = new ServerBootstrap();
    b.group(bossGroup(), workerGroup())
        .channel(NioServerSocketChannel.class)
        .option(ChannelOption.SO_BACKLOG, backlog)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
        .childOption(ChannelOption.SO_KEEPALIVE, true)
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();

            pipeline.addLast("decoder", new HttpRequestDecoder());
            pipeline.addLast("aggregator", new HttpObjectAggregator(1048576));
            pipeline.addLast("encoder", new HttpResponseEncoder());
            pipeline.addLast("router", router);
            pipeline.addLast("transport", stubTransport);
            pipeline.addLast("sessionManager", sessionManager);
          }
        });
    return b;
  }

  @Bean(name = "bossGroup", destroyMethod = "shutdownGracefully")
  public NioEventLoopGroup bossGroup() {
    return new NioEventLoopGroup(bossCount);
  }

  @Bean(name = "workerGroup", destroyMethod = "shutdownGracefully")
  public NioEventLoopGroup workerGroup() {
    return new NioEventLoopGroup(workerCount);
  }

  @Bean(name = "tcpSocketAddress")
  public InetSocketAddress tcpPort() {
    return new InetSocketAddress("0.0.0.0", tcpPort);
  }

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Bean(destroyMethod = "shutdown")
  public ThreadPoolExecutor threadPoolExecutor() {
    ThreadPoolExecutor threadPoolExecutor =  new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS,
        new ArrayBlockingQueue<Runnable>(queueSize));

    threadPoolExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

    return threadPoolExecutor;
  }
}
