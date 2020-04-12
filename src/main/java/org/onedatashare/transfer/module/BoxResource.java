package org.onedatashare.transfer.module;

import org.onedatashare.transfer.model.credential.EndpointCredential;

import java.io.UnsupportedEncodingException;

import static org.onedatashare.transfer.model.core.ODSConstants.BOX_URI_SCHEME;

public class BoxResource extends Resource {
    public BoxResource(EndpointCredential credential) {
        super(credential);
    }

    @Override
    public String pathFromUri(String uri) throws UnsupportedEncodingException {
        String path = "";
        if(uri.contains(BOX_URI_SCHEME)){
            path = uri.substring(BOX_URI_SCHEME.length() - 1);
        }
        path = java.net.URLDecoder.decode(path, "UTF-8");
        return path;
    }

}