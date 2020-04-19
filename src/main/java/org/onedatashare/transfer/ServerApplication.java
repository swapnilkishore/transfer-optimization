package org.onedatashare.transfer;

import org.apache.log4j.BasicConfigurator;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ServerApplication {

  public static void main(String[] args) {
    BasicConfigurator.configure();
    SpringApplication.run(ServerApplication.class, args);
  }
}
