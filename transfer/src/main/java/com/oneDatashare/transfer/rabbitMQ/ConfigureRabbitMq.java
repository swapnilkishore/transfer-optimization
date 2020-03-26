package com.oneDatashare.transfer.rabbitMQ;

import com.oneDatashare.transfer.controller.MessageConsumer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Bean;

public class ConfigureRabbitMq {
    @Bean
    MessageListenerAdapter listenerAdapter(MessageConsumer handler){
        return new MessageListenerAdapter(handler, "recievedMessage");
    }
}
