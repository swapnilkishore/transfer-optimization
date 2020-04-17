package org.onedatashare.transfer.resource;

import com.box.sdk.BoxAPIConnection;
import org.onedatashare.transfer.model.core.IdMap;
import org.onedatashare.transfer.model.credential.EndpointCredential;
import org.onedatashare.transfer.model.drain.Drain;
import org.onedatashare.transfer.model.tap.Tap;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import static org.onedatashare.transfer.model.core.ODSConstants.BOX_URI_SCHEME;

public class BoxResource extends Resource {
    BoxAPIConnection client;
    public BoxResource(EndpointCredential credential) {
        super(credential);
    }
    private transient HashMap<String, String> pathParentMap = new HashMap<>();

    @Override
    public Tap getTap(IdMap idMap, String baseUrl) throws Exception {
        return null;
    }

    @Override
    public Drain getDrain(IdMap idMap, String baseUrl) throws Exception {
        return null;
    }

    @Override
    public String pathFromUri(String uri) throws UnsupportedEncodingException {
        String path = "";
        if(uri.contains(BOX_URI_SCHEME)){
            path = "/" + uri.substring(BOX_URI_SCHEME.length());
        }
        path = java.net.URLDecoder.decode(path, "UTF-8");
        return path;
    }
}
