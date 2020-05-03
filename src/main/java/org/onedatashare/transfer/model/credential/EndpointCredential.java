package org.onedatashare.transfer.model.credential;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;


/**
 * Base class for storing one user credential
 */
@Data
@Document
public class EndpointCredential{
    protected String accountId;
}