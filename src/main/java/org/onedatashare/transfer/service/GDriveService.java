package org.onedatashare.transfer.service;

import org.onedatashare.transfer.model.core.*;
import org.onedatashare.transfer.model.credential.OAuthCredential;
import org.onedatashare.transfer.model.error.TokenExpiredException;
import org.onedatashare.transfer.model.useraction.IdMap;
import org.onedatashare.transfer.model.useraction.UserAction;
import org.onedatashare.transfer.module.googledrive.GoogleDriveSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import static org.onedatashare.transfer.model.core.ODSConstants.*;

@Service
public class GDriveService extends OAuthResourceService {
    @Autowired
    private UserService userService;

    public Mono<? extends Resource> getResourceWithUserActionUri(String cookie, UserAction userAction) {
        final String path = pathFromUri(userAction.getUri());
        String id = userAction.getId();
        ArrayList<IdMap> idMap = userAction.getMap();

        if (userAction.getCredential().isTokenSaved()) {
            return userService.getLoggedInUser(cookie)
                    .handle((usr, sink) -> {
                        this.fetchCredentialsFromUserAction(usr, sink, userAction);
                    })
                    .map(credential -> new GoogleDriveSession(URI.create(userAction.getUri()), (Credential) credential))
                    .flatMap(GoogleDriveSession::initialize)
                    .flatMap(driveSession -> driveSession.select(path, id, idMap))
                    .onErrorResume(throwable -> throwable instanceof TokenExpiredException, throwable ->
                            Mono.just(userService.updateCredential(cookie, userAction.getCredential(), ((TokenExpiredException) throwable).cred))
                                    .map(credential -> new GoogleDriveSession(URI.create(userAction.getUri()), credential))
                                    .flatMap(GoogleDriveSession::initialize)
                                    .flatMap(driveSession -> driveSession.select(path, id, idMap))
                    );
        } else {
            return Mono.just(new OAuthCredential(userAction.getCredential().getToken()))
                    .map(oAuthCred -> new GoogleDriveSession(URI.create(userAction.getUri()), oAuthCred))
                    .flatMap(GoogleDriveSession::initializeNotSaved)
                    .flatMap(driveSession -> driveSession.select(path, id, idMap));
        }
    }

    public String pathFromUri(String uri) {
        String path = "";
        path = uri.substring(DRIVE_URI_SCHEME.length() - 1);
        try {
            path = java.net.URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return path;
    }

    @Override
    public Mono<Stat> list(String cookie, UserAction userAction) {
        return getResourceWithUserActionUri(cookie, userAction).flatMap(Resource::stat);
    }

    @Override
    public Mono<Boolean> mkdir(String cookie, UserAction userAction) {
        return getResourceWithUserActionUri(cookie, userAction)
                .flatMap(Resource::mkdir)
                .flatMap(resource -> ((Resource) resource).stat());
    }

    @Override
    public Mono<Boolean> delete(String cookie, UserAction userAction) {
        return getResourceWithUserActionUri(cookie, userAction)
                .flatMap(Resource::delete)
                .map(v -> true);
    }

    @Override
    public Mono<String> download(String cookie, UserAction userAction) {
        return getResourceWithUserActionUri(cookie, userAction)
                .flatMap(Resource::download);
    }

}
