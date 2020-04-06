package org.onedatashare.transfer.service;


import org.onedatashare.transfer.model.core.*;
import org.onedatashare.transfer.model.credential.OAuthCredential;
import org.onedatashare.transfer.model.error.TokenExpiredException;
import org.onedatashare.transfer.model.useraction.IdMap;
import org.onedatashare.transfer.model.useraction.UserAction;
import org.onedatashare.transfer.module.box.BoxResource;
import org.onedatashare.transfer.module.box.BoxSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;


@Service
public class BoxService extends OAuthResourceService {

    @Autowired
    private UserService userService;

    @Override
    public Mono<Stat> list(String cookie, UserAction userAction) {
        return getBoxResourceUserActionUri(cookie, userAction).flatMap(BoxResource::stat);
    }

    @Override
    public Mono<Boolean> mkdir(String cookie, UserAction userAction) {
        return getBoxResourceUserActionUri(cookie, userAction)
                .flatMap(BoxResource::mkdir)
                .map(r -> true);
    }

    @Override
    public Mono<Boolean> delete(String cookie, UserAction userAction) {
        return getBoxResourceUserActionUri(cookie, userAction)
                .flatMap(BoxResource::delete)
                .map(val -> true);
    }

    @Override
    public Mono<String> download(String cookie, UserAction userAction) {
        return getBoxResourceUserActionUri(cookie, userAction)
                .flatMap(BoxResource::download);
    }

    public Mono<BoxResource> getBoxResourceUserActionUri(String cookie, UserAction userAction) {
        final String path = pathFromUri(userAction.getUri());
        String id = userAction.getId();
        ArrayList<IdMap> idMap = userAction.getMap();
        if (userAction.getCredential().isTokenSaved()) {
            return userService.getLoggedInUser(cookie)
                    .handle((usr, sink) -> {
                        this.fetchCredentialsFromUserAction(usr, sink, userAction);
                    })
                    .map(credential -> new BoxSession(URI.create(userAction.getUri()), (Credential) credential))
                    .flatMap(BoxSession::initialize)
                    .flatMap(boxSession -> boxSession.select(path, id, idMap))
                    .onErrorResume(throwable -> throwable instanceof TokenExpiredException, throwable -> {
                        userService.deleteBoxCredential(cookie, userAction.getCredential(), ((TokenExpiredException) throwable).cred).subscribe();
                        return null;
                    });
        } else {
            return Mono.just(new OAuthCredential(userAction.getCredential().getToken()))
                    .map(oAuthCred -> new BoxSession(URI.create(userAction.getUri()), oAuthCred))
                    .flatMap(BoxSession::initializeNotSaved)
                    .flatMap(boxSession -> boxSession.select(path, id, idMap));
        }
    }

    public String pathFromUri(String uri) {
        String path = "";
        if(uri.contains(ODSConstants.BOX_URI_SCHEME)){
            path = uri.substring(ODSConstants.BOX_URI_SCHEME.length() - 1);
        }
        try {
            path = java.net.URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return path;
    }
}

