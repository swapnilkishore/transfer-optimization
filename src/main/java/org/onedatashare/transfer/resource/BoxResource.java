package org.onedatashare.transfer.resource;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIRequest;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.http.HttpMethod;
import org.onedatashare.transfer.model.core.EntityInfo;
import org.onedatashare.transfer.model.credential.EndpointCredential;
import org.onedatashare.transfer.model.credential.OAuthEndpointCredential;
import org.onedatashare.transfer.model.drain.BoxDrain;
import org.onedatashare.transfer.model.drain.Drain;
import org.onedatashare.transfer.model.tap.BoxTap;
import org.onedatashare.transfer.model.tap.Tap;

import javax.swing.*;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.onedatashare.transfer.model.core.ODSConstants.BOX_URI_SCHEME;

public final class BoxResource extends Resource {
    private BoxAPIConnection client;
    private HashMap<String, String> pathIdMap;

    private static final String ROOT_ID = "0";
    public BoxResource(EndpointCredential credential) {
        super(credential);
        this.client = new BoxAPIConnection(((OAuthEndpointCredential) credential).getToken());
        this.pathIdMap = new HashMap<>();
    }

    /**
     * Creates box folders recursively if parent is not present and saves it in the pathIdMap. This has to be
     * synchronized to avoid creating
     * multipe folders with same name (which might lead to error for box). Using concurrent hashmap may not be a good
     * option as the computeIfAbsent function will be called recursively
     * @param path : Path to be created
     * @return id of the newly created path
     */
    //TODO: add code to check if a sub folder already exists (this case not handled currently)
    private synchronized String getParentId(final String parentId, final String path){
        String lastParentId = parentId;
        for(int index = path.indexOf("/"); index != -1; index = path.indexOf("/", index + 1)){
            String tempPath = path.substring(0, index + 1);
            String tempPathId = this.pathIdMap.get(tempPath);
            if(tempPathId == null){
                tempPathId = new BoxFolder(this.client, lastParentId).getID();
                this.pathIdMap.put(tempPath, tempPathId);
            }
            lastParentId = tempPathId;
        }
        return lastParentId;
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
        String filePath = this.pathFromUri(baseInfo.getPath() + relativeInfo.getPath());
        BoxFolder folder;
//        if(this.pathFromUri(baseInfo.getPath()).equals("/")){
//            folder = BoxFolder.getRootFolder(client);
//        } else {
//            String parentId = this.getParentId(baseInfo.getId(), relativeInfo.getPath());
//            folder = new BoxFolder(client, parentId);
//        }
        //Added for dev
        folder = BoxFolder.getRootFolder(client);
        String fileName = filePath;
        int lastIndexOfSlash = fileName.lastIndexOf("/");
        if(lastIndexOfSlash != -1){
            fileName = fileName.substring(lastIndexOfSlash + 1);
        }
        return BoxDrain.getInstance(folder, fileName, 21295967);
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
