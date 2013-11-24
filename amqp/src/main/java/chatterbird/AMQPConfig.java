package chatterbird;


import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@Import(ApplicationConfig.class)
@PropertySource("classpath:amqp.properties")
public class AMQPConfig {
  @Autowired
  private Environment env;

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
  public SimpleMessageListenerContainer messageListenerContainer(CachingConnectionFactory connectionFactory, ThreadPoolExecutor threadPoolExecutor, Queue broadcastQueue) {
    SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.setQueues(broadcastQueue);
    container.setTaskExecutor(threadPoolExecutor);
    container.setMessageListener(exampleListener());

    return container;
  }

  /*@Bean
  @Autowired
  public RabbitTemplate broadcastBroker(CachingConnectionFactory connectionFactory) {
    RabbitTemplate broadcastBroker = new RabbitTemplate(connectionFactory);
    broadcastBroker.setExchange("chatterbird.broadcast");
    //broadcastBroker.setMessageConverter();
    return broadcastBroker;
  }*/

  @Bean
  public FanoutExchange broadcastExchange() {
    FanoutExchange broadcastExchange = new FanoutExchange("chatterbird.broadcast");
    broadcastExchange.setShouldDeclare(false);
    return broadcastExchange;
  }

  @Bean
  @Autowired
  public RabbitAdmin rabbitAdmin(CachingConnectionFactory connectionFactory) {
    RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
    return rabbitAdmin;
  }

  @Bean
  public CachingConnectionFactory connectionFactory() {
    CachingConnectionFactory connectionFactory = new CachingConnectionFactory(env.getProperty("amqp.host"));
    connectionFactory.setUsername(env.getProperty("amqp.username"));
    connectionFactory.setPassword(env.getProperty("amqp.password"));

    return connectionFactory;
  }

  @Bean
  @Autowired
  public Queue broadcastQueue(RabbitAdmin rabbitAdmin) {
    //TODO: This is bullshit
    Queue queue = new Queue(env.getProperty("amqp.broadcastPrefix") + UUID.randomUUID().toString(), true, true, true);
    queue.setAdminsThatShouldDeclare(rabbitAdmin);
    return  queue;
  }
}
