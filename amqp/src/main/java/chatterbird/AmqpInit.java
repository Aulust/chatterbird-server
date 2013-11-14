package chatterbird;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.UniquelyNamedQueue;
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
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class AmqpInit {
  @Autowired
  private RabbitAdmin rabbitAdmin;
  @Autowired
  private UniquelyNamedQueue broadcastQueue;

  @PostConstruct
  public void initQueues() {
    //rabbitAdmin.declareQueue(broadcastQueue);
    //rabbitAdmin.declareBinding(new Binding());
  }
}
