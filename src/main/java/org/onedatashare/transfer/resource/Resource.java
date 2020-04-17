package org.onedatashare.transfer.resource;

import lombok.NoArgsConstructor;
import org.onedatashare.transfer.model.core.IdMap;
import org.onedatashare.transfer.model.credential.EndpointCredential;
import org.onedatashare.transfer.model.drain.Drain;
import org.onedatashare.transfer.model.tap.Tap;

import java.io.UnsupportedEncodingException;

@NoArgsConstructor
public abstract class Resource {
    EndpointCredential credential;

    Resource(EndpointCredential credential){
        this.credential = credential;
    }

    public abstract Tap getTap(IdMap idMap, String baseUrl) throws Exception;

    public abstract Drain getDrain(IdMap idMap, String baseUrl) throws Exception;

    public String pathFromUri(String uri) throws UnsupportedEncodingException {
        String path = "";
        path = java.net.URLDecoder.decode(path, "UTF-8");
        return path;
    }
}
