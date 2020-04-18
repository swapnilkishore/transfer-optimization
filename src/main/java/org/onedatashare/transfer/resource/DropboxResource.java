package org.onedatashare.transfer.resource;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.FileMetadata;
import org.onedatashare.transfer.model.core.EntityInfo;
import org.onedatashare.transfer.model.credential.EndpointCredential;
import org.onedatashare.transfer.model.credential.OAuthEndpointCredential;
import org.onedatashare.transfer.model.drain.Drain;
import org.onedatashare.transfer.model.drain.DropboxDrain;
import org.onedatashare.transfer.model.tap.DropboxTap;
import org.onedatashare.transfer.model.tap.Tap;

import java.io.UnsupportedEncodingException;

import static org.onedatashare.transfer.model.core.ODSConstants.DROPBOX_URI_SCHEME;

public class DropboxResource extends Resource {
    private DbxClientV2 client;

//    @Value("${dropbox.identifier}")
    private String DROPBOX_CLIENT_IDENTIFIER = "OneDataShare-DIDCLab";

    public DropboxResource(EndpointCredential credential) {
        super(credential);
        DbxRequestConfig config = DbxRequestConfig.newBuilder(DROPBOX_CLIENT_IDENTIFIER).build();
        this.client = new DbxClientV2(config, ((OAuthEndpointCredential) credential).getToken());
    }

    @Override
    public Tap getTap(EntityInfo baseInfo, EntityInfo relativeInfo) throws Exception {
        String url = this.pathFromUri(baseInfo.getPath() + relativeInfo.getPath());
        DbxUserFilesRequests requests = this.client.files();
        FileMetadata metaData = (FileMetadata) requests.getMetadata(url);
        long size = metaData.getSize();
        return DropboxTap.initialize(url, requests, size);
    }

    @Override
    public Drain getDrain(EntityInfo baseInfo, EntityInfo relativeInfo) throws Exception {
        String url = this.pathFromUri(baseInfo.getPath() + relativeInfo.getPath());
        DbxUserFilesRequests requests = this.client.files();
        return DropboxDrain.initialize(url, requests);
    }

    @Override
    public String pathFromUri(String uri) throws UnsupportedEncodingException {
        String path = "";
        if(uri.contains(DROPBOX_URI_SCHEME)){
            //Dropbox root starts with a "/"
            path = "/" + uri.substring(DROPBOX_URI_SCHEME.length());
        }
        path = java.net.URLDecoder.decode(path, "UTF-8");
        return path;
    }
}
