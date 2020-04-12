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
    public static abstract class BaseSD {
        protected EndpointType type;
        protected String credId;
    }

    @Data
    public static class Destination extends BaseSD {
        private String id;
        private String uri;
    }

    @Data
    public static class Source extends BaseSD {
        private String[] idList;
        private String[] uriList;
    }
}