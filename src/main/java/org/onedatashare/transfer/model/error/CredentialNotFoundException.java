package org.onedatashare.transfer.model.error;

import org.onedatashare.transfer.model.core.EndpointType;

public class CredentialNotFoundException extends Exception{
    public CredentialNotFoundException(){
        super("Credential not found for transfer");
    }

    public CredentialNotFoundException(EndpointType type, String id){
        super(String.format("Credential %s/%s not found",type, id));
    }
}
