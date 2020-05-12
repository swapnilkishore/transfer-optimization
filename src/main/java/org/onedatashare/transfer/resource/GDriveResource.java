package org.onedatashare.transfer.resource;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.services.drive.Drive;
import org.onedatashare.transfer.config.GDriveConfig;
import org.onedatashare.transfer.model.core.EntityInfo;
import org.onedatashare.transfer.model.credential.EndpointCredential;
import org.onedatashare.transfer.model.credential.OAuthEndpointCredential;
import org.onedatashare.transfer.model.drain.Drain;
import org.onedatashare.transfer.model.tap.GDriveTap;
import org.onedatashare.transfer.model.tap.Tap;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;

import static org.onedatashare.transfer.model.core.ODSConstants.DRIVE_URI_SCHEME;

public final class GDriveResource extends Resource {
    public static final String ROOT_DIR_ID = "root";

    public static GDriveConfig gDriveConfig = GDriveConfig.getInstance();
    private static final String USER_ID = "user";
    private static final String DOWNLOAD_URL = "https://www.googleapis.com/drive/v3/files/{}?alt=media";
    private Drive driveService;

    public GDriveResource(EndpointCredential cred) throws IOException {
        super(cred);
        OAuthEndpointCredential oAuthEndpointCredential = (OAuthEndpointCredential) this.credential;
        TokenResponse tokenResponse = new TokenResponse()
                .setAccessToken(oAuthEndpointCredential.getToken())
                .setRefreshToken(oAuthEndpointCredential.getRefreshToken())
                .setExpiresInSeconds(Math.min(0,
                        oAuthEndpointCredential.getExpiresAt().toInstant().getEpochSecond()) -
                        Instant.now().getEpochSecond());
        Credential credential = gDriveConfig.getFlow().createAndStoreCredential(tokenResponse, USER_ID);
        this.driveService =  new Drive.Builder(
                gDriveConfig.getHttpTransport(), gDriveConfig.getJsonFactory(), gDriveConfig.setHttpTimeout(credential))
                .setApplicationName(gDriveConfig.getAppName())
                .build();

    }

    @Override
    public Tap getTap(EntityInfo baseInfo, EntityInfo relativeInfo) throws Exception {
        String downloadUrl = "https://www.googleapis.com/drive/v3/files/{}?alt=media".replace("{}", relativeInfo.getId());
        HttpRequest httpRequestGet = driveService.getRequestFactory().buildGetRequest(new GenericUrl(downloadUrl));
        System.out.println("Size is " + httpRequestGet.getHeaders().size());
        System.out.println("Size is " + httpRequestGet.getContent().getLength());
        return GDriveTap.initialize(httpRequestGet, relativeInfo.getSize());
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
