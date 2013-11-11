package chatterbird;


import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@Import(ApplicationConfig.class)
@PropertySource("classpath:amqp.properties")
public class AMQPConfig {
  @Autowired
  private Environment env;

  @Bean(name = "sdsd")
  public String message() {
    System.out.println("dsfsdfsdf");
    return new String("fsdfsadf");
  }

  @Bean
  public MessageListener exampleListener() {
    return new MessageListener() {
      public void onMessage(Message message) {
        System.out.println("received: " + message);
      }
    };
  }

  @Bean
  @Autowired
  public SimpleMessageListenerContainer messageListenerContainer(CachingConnectionFactory connectionFactory, ThreadPoolExecutor threadPoolExecutor) {
    System.out.println(threadPoolExecutor);
    SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.setQueueNames("some.queue");
    container.setTaskExecutor(threadPoolExecutor);
    container.setMessageListener(exampleListener());

    return container;
  }

  @Bean
  public CachingConnectionFactory connectionFactory() {
    System.out.println("dsfsdfsdf");
    CachingConnectionFactory connectionFactory = new CachingConnectionFactory(env.getProperty("amqp.host"));
    connectionFactory.setUsername(env.getProperty("amqp.username"));
    connectionFactory.setPassword(env.getProperty("amqp.password"));

    return connectionFactory;
  }
}
