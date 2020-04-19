package org.onedatashare.transfer.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class TransferOptions implements Serializable {
    private Boolean compress;
    private Boolean encrypt;
    private String optimizer;
    private boolean overwrite;
    private Integer retry;
    private Boolean verify;
}
