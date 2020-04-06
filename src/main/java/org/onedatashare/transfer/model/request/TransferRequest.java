package org.onedatashare.transfer.model.request;

import lombok.Data;
import org.onedatashare.transfer.model.useraction.UserActionResource;

@Data
public class TransferRequest {
    UserActionResource src;
    UserActionResource dest;
    TransferOptions options;
}
