package org.oneDatashare.transfer.controller;

import org.oneDatashare.transfer.model.core.Job;
import org.oneDatashare.transfer.service.ResourceServiceImpl;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessageConsumer {
    @Autowired
    private ResourceServiceImpl resourceService;
    @RabbitListener(queues="Rishabh's Queue")
    public void recievedMessage(Job receivedJob) {
        // Make this a return statement after changing the return type in resourceServiceImpl
        resourceService.submit(receivedJob);
    }
}
