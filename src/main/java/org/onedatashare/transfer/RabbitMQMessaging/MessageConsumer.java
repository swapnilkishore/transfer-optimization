//package org.onedatashare.transfer.RabbitMQMessaging;
//
//import org.onedatashare.transfer.ServerApplication;
//import org.onedatashare.transfer.model.request.TransferJobRequest;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//@Service
//public class MessageConsumer {
//    @Autowired
//    //private PayloadRepository payloadRepository;
//    private static final Logger log = LoggerFactory.getLogger(MessageConsumer.class);
//
//    @RabbitListener(queues = ServerApplication.QUEUE_SPECIFIC_NAME)
//    public void receiveMessage(String jobRequest) {
//     //   payloadRepository.saveAll(Flux.just(payloadMessage)).subscribe();
//        log.info("Received message as specific class: {}", jobRequest);
//    }
//}
