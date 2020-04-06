package org.onedatashare.transfer.model.request;

import lombok.Data;
import org.onedatashare.transfer.model.useraction.UserActionCredential;

@Data
public class RequestData {
    private String uri;
    private String id;
    private String portNumber;
    private String type;
    private UserActionCredential credential;
}
