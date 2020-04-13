package org.onedatashare.transfer.model.request;

import lombok.Data;
import lombok.NonNull;
import org.onedatashare.transfer.model.core.EndpointType;

import java.util.ArrayList;

@Data
public class TransferJobRequest {
    @NonNull private String id;
    @NonNull private Source source;
    @NonNull private Destination destination;
    private TransferOptions options;

    @Data
    public static class Destination {
        protected EndpointType type;
        protected String credId;
        protected String baseId;
        protected String baseUrl;
    }

    @Data
    public static class Source extends Destination {
        private String[] idList;
        private String[] urlList;
    }
}