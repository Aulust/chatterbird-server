package chatterbird;


import chatterbird.listener.EventListener;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
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
  @Autowired
  private EventListener eventListener;

  @Bean
  @Autowired
  public SimpleMessageListenerContainer messageListenerContainer(CachingConnectionFactory connectionFactory,
      ThreadPoolExecutor threadPoolExecutor, Queue broadcastQueue, Queue incomingQueue) {
    SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.setQueues(broadcastQueue, incomingQueue);
    container.setTaskExecutor(threadPoolExecutor);
    //TODO: Actually it would be match better to implement message converter witch will reject messages with invalid format
    container.setMessageListener(new MessageListenerAdapter(eventListener, new Jackson2JsonMessageConverter()));
    container.setErrorHandler(eventListener);

    return container;
  }

  @Bean(name = "broadcastTemplate")
  public RabbitTemplate rabbitTemplate() {
    RabbitTemplate broadcastTemplate = new RabbitTemplate(connectionFactory());
    broadcastTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
    broadcastTemplate.setExchange(env.getProperty("amqp.broadcast.exchange"));

    return broadcastTemplate;
  }

  @Bean
  public FanoutExchange broadcastExchange() {
    return new FanoutExchange(env.getProperty("amqp.broadcast.exchange"));
  }

  @Bean
  public DirectExchange incomingExchange() {
    return new DirectExchange(env.getProperty("amqp.incoming.exchange"));
  }

  @Bean
  @Autowired
  public RabbitAdmin rabbitAdmin(CachingConnectionFactory connectionFactory) {
    return new RabbitAdmin(connectionFactory);
  }

  @Bean
  @Autowired
  public Binding broadcastBinding(Queue broadcastQueue, FanoutExchange broadcastExchange) {
    return BindingBuilder.bind(broadcastQueue).to(broadcastExchange);
  }

  @Bean
  @Autowired
  public Binding incomingBinding(Queue incomingQueue, DirectExchange incomingExchange) {
    return BindingBuilder.bind(incomingQueue).to(incomingExchange).withQueueName();
  }

  @Bean
  public CachingConnectionFactory connectionFactory() {
    CachingConnectionFactory connectionFactory = new CachingConnectionFactory(env.getProperty("amqp.host"));
    connectionFactory.setUsername(env.getProperty("amqp.username"));
    connectionFactory.setPassword(env.getProperty("amqp.password"));

    return connectionFactory;
  }

  @Bean
  public Queue broadcastQueue() {
    //TODO: This is bullshit
    return new Queue(env.getProperty("amqp.broadcast.queue.prefix") + UUID.randomUUID().toString(), false, true, true);
  }

  @Bean
  public Queue incomingQueue() {
    return new Queue(env.getProperty("amqp.incoming.queue"), true);
  }
}
