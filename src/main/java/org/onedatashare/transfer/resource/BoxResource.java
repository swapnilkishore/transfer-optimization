package org.onedatashare.transfer.resource;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIRequest;
import com.box.sdk.BoxFile;
import com.box.sdk.http.HttpMethod;
import org.onedatashare.transfer.model.core.EntityInfo;
import org.onedatashare.transfer.model.credential.EndpointCredential;
import org.onedatashare.transfer.model.credential.OAuthEndpointCredential;
import org.onedatashare.transfer.model.drain.Drain;
import org.onedatashare.transfer.model.tap.BoxTap;
import org.onedatashare.transfer.model.tap.Tap;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;

import static org.onedatashare.transfer.model.core.ODSConstants.BOX_URI_SCHEME;

public final class BoxResource extends Resource {
    BoxAPIConnection client;

    public BoxResource(EndpointCredential credential) {
        super(credential);
        client = new BoxAPIConnection(((OAuthEndpointCredential) credential).getToken());
    }

    private transient HashMap<String, String> pathParentMap = new HashMap<>();

    @Override
    public Tap getTap(EntityInfo baseInfo, EntityInfo relativeInfo) throws Exception {
        BoxFile file = new BoxFile(client, relativeInfo.getId());
        URL downloadUrl = file.getDownloadURL();
        long size = file.getInfo().getSize();
        BoxAPIRequest request = new BoxAPIRequest(client, downloadUrl, HttpMethod.GET);
        return BoxTap.initialize(request, size);
    }

    @Override
    public Drain getDrain(EntityInfo baseInfo, EntityInfo relativeInfo) throws Exception {
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
