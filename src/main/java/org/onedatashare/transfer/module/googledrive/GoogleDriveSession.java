package org.onedatashare.transfer.module.googledrive;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.onedatashare.transfer.model.core.CredentialOld;
import org.onedatashare.transfer.model.core.Session;
import org.onedatashare.transfer.model.credentialold.OAuthCredentialOld;
import org.onedatashare.transfer.model.error.AuthenticationRequired;
import org.onedatashare.transfer.model.error.TokenExpiredException;
import org.onedatashare.transfer.model.useraction.IdMap;
//import org.onedatashare.transfer.service.ODSLoggerService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.util.*;

@Getter(AccessLevel.PROTECTED)
@Setter(AccessLevel.PUBLIC)
public class GoogleDriveSession extends Session<GoogleDriveSession, GoogleDriveResource> {

    @Autowired
    GDriveConfig driveConfig;

    private Drive service;
    private transient HashMap<String, String> pathToParentIdMap = new HashMap<>();
    protected ArrayList<IdMap> idMap = null;

    public GoogleDriveSession() {
    }

    public GoogleDriveSession(URI uri, CredentialOld credentialOld) {
        super(uri, credentialOld);
    }

    @Override
    public Mono<GoogleDriveResource> select(String path) {
        return Mono.just(new GoogleDriveResource(this, path));
    }

    @Override
    public Mono<GoogleDriveResource> select(String path, String id, ArrayList<IdMap> idMap) {
        this.idMap = idMap;
        if (idMap != null && idMap.size() > 0)
            for (IdMap idPath : idMap) {
                pathToParentIdMap.put(idPath.getPath(), idPath.getId());
            }
        return Mono.just(new GoogleDriveResource(this, path, id));
    }

    /**
     * This method is used for initializing googleDriveSession when OAuth tokens are not saved in the backend
     * It skips refresh token check as refresh tokens are not stored in the front-end
     */
    public Mono<GoogleDriveSession> initializeNotSaved() {
        return Mono.create(s -> {
            if (getCredentialOld() instanceof OAuthCredentialOld) {
                try {
                    service = getDriveService(((OAuthCredentialOld) getCredentialOld()).token);
                } catch (Throwable t) {
                    s.error(t);
                }
                if (service == null) {
//                    ODSLoggerService.logError("Token has expired for the user");
                    s.error(new TokenExpiredException(null, "Invalid token"));
                } else {
                    s.success(this);
                }
            } else
                s.error(new AuthenticationRequired("oauth"));
        });
    }

    @Override
    public Mono<GoogleDriveSession> initialize() {
        return Mono.create(s -> {
            if (getCredentialOld() instanceof OAuthCredentialOld) {
                try {
                    service = getDriveService(((OAuthCredentialOld) getCredentialOld()).token);
                } catch (Throwable t) {
                    s.error(t);
                }
                Date currentTime = new Date();
                if (service != null && ((OAuthCredentialOld) getCredentialOld()).expiredTime != null &&
                        currentTime.before(((OAuthCredentialOld) getCredentialOld()).expiredTime))
                    s.success(this);
                else {
                    OAuthCredentialOld newCredential = updateToken();
                    if (newCredential.refreshToken != null)
                        s.error(new TokenExpiredException(newCredential, "Token has expired"));
                }
            } else s.error(new AuthenticationRequired("oauth"));
        });
    }

    public com.google.api.client.auth.oauth2.Credential authorize(String token) throws IOException {
        com.google.api.client.auth.oauth2.Credential credential = driveConfig.getFlow().loadCredential(token);
        return credential;
    }

    private static HttpRequestInitializer setHttpTimeout(final HttpRequestInitializer requestInitializer) {
        return new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest httpRequest) {
                try {
                    requestInitializer.initialize(httpRequest);
                    httpRequest.setConnectTimeout(3 * 60000);  // 3 minutes connect timeout
                    httpRequest.setReadTimeout(3 * 60000);  // 3 minutes read timeout
                } catch (IOException ioe) {
//                    ODSLoggerService.logError("IOException occurred in GoogleDriveSession.setHttpTimeout()", ioe);
                } catch (NullPointerException npe) {
//                    ODSLoggerService.logError("IOException occurred in GoogleDriveSession.setHttpTimeout()", npe);
                }
            }
        };
    }

    public Drive getDriveService(String token) throws IOException {
        com.google.api.client.auth.oauth2.Credential credential = authorize(token);
        if (credential == null) {
            return null;
        }
        return new Drive.Builder(
                GDriveConfig.getHttpTransport(), GDriveConfig.getJsonFactory(), setHttpTimeout(credential))
                .setApplicationName("OneDataShare")
                .build();
    }

    public OAuthCredentialOld updateToken() {
        // Updating the access token for googledrive using refresh token
        OAuthCredentialOld cred = (OAuthCredentialOld) getCredentialOld();
        try {
            //GoogleCredential refreshTokenCredential = new GoogleCredential.Builder().setJsonFactory(JSON_FACTORY).setTransport(HTTP_TRANSPORT).setClientSecrets(c.client_id, c.client_secret).build().setRefreshToken(cred.refreshToken);
            TokenResponse response = new GoogleRefreshTokenRequest(new NetHttpTransport(), new JacksonFactory(),
                    cred.refreshToken, driveConfig.getClientId(), driveConfig.getClientSecret()).execute();

            cred.token = response.getAccessToken();

            Calendar calendar = Calendar.getInstance(); // gets a calendar using the default time zone and locale.
            calendar.add(Calendar.SECOND, response.getExpiresInSeconds().intValue());

            cred.expiredTime = calendar.getTime();

            driveConfig.getFlow().createAndStoreCredential(response, cred.token);
//            ODSLoggerService.logInfo("New AccessToken and RefreshToken fetched");
        } catch (com.google.api.client.auth.oauth2.TokenResponseException te) {
            cred.refreshTokenExp = true;
//            ODSLoggerService.logError("Refresh token for the user has expired");
        } catch (IOException e) {
            e.printStackTrace();
//            ODSLoggerService.logError("IOException in update Token");
        }
        return cred;
    }

}
