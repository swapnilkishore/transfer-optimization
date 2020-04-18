package org.onedatashare.transfer.resource;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIRequest;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
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
import java.util.concurrent.ConcurrentHashMap;

import static org.onedatashare.transfer.model.core.ODSConstants.BOX_URI_SCHEME;

public final class BoxResource extends Resource {
    private BoxAPIConnection client;
    private HashMap<String, String> pathIdMap;

    public BoxResource(EndpointCredential credential) {
        super(credential);
        this.client = new BoxAPIConnection(((OAuthEndpointCredential) credential).getToken());
        this.pathIdMap = new HashMap<>();
    }

    /**
     * Creates box folders recursively and saves it in the pathIdMap. This has to be synchronized to avoid creating
     * multipe folders with same name (which might lead to error for box). Using concurrent hashmap may not be a good
     * option as the computeIfAbsent function will be called recursively
     * @param path : Path to be created
     * @return id of the newly created path
     */
    private synchronized String createFolderRecursively(String path){
        String finalId = null;
        for(int index = path.indexOf("/"); index != -1; index = path.indexOf("/", index + 1)){
            String tempPath = path.substring(0, index + 1);
            String tempPathId = pathIdMap.get(tempPath);
            if(tempPathId == null){
                tempPathId = String.valueOf(tempPath.hashCode());
                pathIdMap.put(tempPath, tempPathId);
            }
            finalId = tempPathId;
        }
        return finalId;
    }


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
