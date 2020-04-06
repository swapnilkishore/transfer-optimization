package org.onedatashare.transfer.module.googledrive;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Data
public class GDriveConfig {

    @Value("${gdrive.authUri}")
    private String authUri;

    @Value("${gdrive.tokenUri}")
    private String tokenUri;

    @Value("${gdrive.authProviderUri}")
    private String authProviderX509CertUrl;

    @Value("${gdrive.redirectUri}")
    private String redirectUri;

    @Value("${gdrive.clientId}")
    private String clientId;

    @Value("${gdrive.clientSecret}")
    private String clientSecret;

    @Value("${gdrive.projectId}")
    private String projectId;

    private GoogleClientSecrets driveClientSecrets;
    private GoogleAuthorizationCodeFlow flow;

    private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE_READONLY);
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static FileDataStoreFactory DATA_STORE_FACTORY;
    private static HttpTransport HTTP_TRANSPORT;

    static {
        try{
            File DATA_STORE_DIR = new File(System.getProperty("user.home"), ".credentials/ods");
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static final HttpTransport getHttpTransport(){
        return HTTP_TRANSPORT;
    }

    public static final JsonFactory getJsonFactory(){
        return JSON_FACTORY;
    }

    public static final FileDataStoreFactory getDATA_STORE_FACTORY(){
        return DATA_STORE_FACTORY;
    }

    @PostConstruct
    public void initialize() {

        if (getClientId() != null || getClientSecret() != null || getTokenUri() != null || getRedirectUri() != null){
            GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();

            details.setAuthUri(authUri).setClientId(clientId)
                    .setClientSecret(clientSecret).setRedirectUris(Arrays.asList(redirectUri))
                    .setTokenUri(tokenUri);
            driveClientSecrets = new GoogleClientSecrets().setInstalled(details);
        }


        try {
            flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, driveClientSecrets, SCOPES)
                    .setDataStoreFactory(DATA_STORE_FACTORY)
                    .build();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
