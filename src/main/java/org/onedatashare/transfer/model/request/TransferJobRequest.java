package org.onedatashare.transfer.model.request;

import lombok.Data;
import org.onedatashare.transfer.model.core.CredentialOld;
import org.onedatashare.transfer.model.core.EndpointType;

import java.util.ArrayList;

@Data
public class TransferJobRequest {
    private String id;
    private Source source;
    private Destination destination;

    @Data
    public static class Destination {
        private EndpointType type;
        private String id;
        private String uri;
        private String credId;
        private transient CredentialOld credentialOld;
    }

    @Data
    public static class Source {
        private EndpointType type;
        private ArrayList<String> idList;
        private ArrayList<String> uriList;
        private String credId;
        private transient CredentialOld credentialOld;
    }
}