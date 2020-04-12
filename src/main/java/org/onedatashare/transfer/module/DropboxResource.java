package org.onedatashare.transfer.module;

import org.onedatashare.transfer.model.core.IdMap;
import org.onedatashare.transfer.model.credential.EndpointCredential;
import org.onedatashare.transfer.model.drain.Drain;
import org.onedatashare.transfer.model.tap.Tap;

import java.io.UnsupportedEncodingException;

import static org.onedatashare.transfer.model.core.ODSConstants.DROPBOX_URI_SCHEME;

public class DropboxResource extends Resource {

    public DropboxResource(EndpointCredential credential) {
        super(credential);
    }

    @Override
    public Tap getTap(IdMap idMap) {
        return null;
    }

    @Override
    public Drain getDrain(IdMap idMap) {
        return null;
    }

    @Override
    public String pathFromUri(String uri) throws UnsupportedEncodingException {
        String path = "";
        if(uri.contains(DROPBOX_URI_SCHEME)){
            path = uri.substring(DROPBOX_URI_SCHEME.length() - 1);
        }
        path = java.net.URLDecoder.decode(path, "UTF-8");
        return path;
    }

}
