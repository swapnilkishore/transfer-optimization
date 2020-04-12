package org.onedatashare.transfer.model.request;

import lombok.Data;
import org.onedatashare.transfer.model.core.EndpointType;

import java.util.ArrayList;

@Data
public class TransferJobRequest {
    private String id;
    private Source source;
    private Destination destination;

    @Data
    public static abstract class BaseSD {
        private EndpointType type;
        private String credId;
    }

    @Data
    public static class Destination extends BaseSD {
        private String id;
        private String uri;
    }

    @Data
    public static class Source extends BaseSD {
        private EndpointType type;
        private ArrayList<String> idList;
        private ArrayList<String> uriList;
        private String credId;
    }
}