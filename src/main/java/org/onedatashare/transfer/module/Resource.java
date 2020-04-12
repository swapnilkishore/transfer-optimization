package org.onedatashare.transfer.module;

import lombok.NoArgsConstructor;
import org.onedatashare.transfer.model.credential.EndpointCredential;

@NoArgsConstructor
public class Resource {
    EndpointCredential credential;

    Resource(EndpointCredential credential){
        this.credential = credential;
    }
}
