//package org.onedatashare.transfer.RabbitMQMessaging;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//
//import java.io.Serializable;
//
//public class PayloadMessage implements Serializable {
//    private final int id;
//    private final Source source;
//    private final Destination destination;
//
//    public PayloadMessage(@JsonProperty("id") int id,
//                          @JsonProperty("source") Source source,
//                          @JsonProperty("destination") Destination destination) {
//        this.id=id;
//        this.source = source;
//        this.destination=destination;
//    }
//
//    public int getId() {
//        return id;
//    }
//
//    public Source getSource() {
//        return source;
//    }
//
//    public Destination getDestination() {
//        return destination;
//    }
//
//
//    @Override
//    public String toString() {
//        return "CustomMessage{" +
//                "id " + id
//                + ", source type " + source.endpointType
//                + ", source credID " +source.credId
//                + ", uriList " + source.uriList.get(0)
//                + ", destination type " + destination.endpointType
//                +", destination credID " +destination.credId
//                + ", destination uri=" + destination.uri ;
//    }
//}
