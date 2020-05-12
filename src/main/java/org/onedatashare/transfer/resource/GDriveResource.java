package org.onedatashare.transfer.resource;

import org.onedatashare.transfer.model.core.EntityInfo;
import org.onedatashare.transfer.model.credential.EndpointCredential;
import org.onedatashare.transfer.model.drain.Drain;
import org.onedatashare.transfer.model.tap.Tap;

import java.io.UnsupportedEncodingException;

import static org.onedatashare.transfer.model.core.ODSConstants.DRIVE_URI_SCHEME;

public final class GDriveResource extends Resource {
    public static final String ROOT_DIR_ID = "root";

    public GDriveResource(EndpointCredential cred) {
        super(cred);
    }

    @Override
    public Tap getTap(EntityInfo baseInfo, EntityInfo relativeInfo) throws Exception {
        return null;
    }

    @Override
    public Drain getDrain(EntityInfo baseInfo, EntityInfo relativeInfo) throws Exception {
        return null;
    }

    @Override
    public String pathFromUri(String uri) throws UnsupportedEncodingException {
        String path = "";
        if(uri.contains(DRIVE_URI_SCHEME)){
            path = uri.substring(DRIVE_URI_SCHEME.length() - 1);
        }
        path = java.net.URLDecoder.decode(path, "UTF-8");
        return path;
    }

}
