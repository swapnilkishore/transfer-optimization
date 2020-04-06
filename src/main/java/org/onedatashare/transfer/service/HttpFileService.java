package org.onedatashare.transfer.service;

import org.onedatashare.transfer.model.core.ODSConstants;
import org.onedatashare.transfer.model.core.Stat;
import org.onedatashare.transfer.model.error.UnsupportedOperationException;
import org.onedatashare.transfer.model.useraction.UserAction;
import org.onedatashare.transfer.module.http.HttpResource;
import org.onedatashare.transfer.module.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URI;

@Service
public class HttpFileService extends ResourceService {
    @Autowired
    private UserService userService;

    public Mono<HttpResource> getResourceWithUserActionUri(String cookie, UserAction userAction) {
        String path = pathFromUri(userAction.getUri());
        return userService.getLoggedInUser(cookie)
                .map(user -> new HttpSession(URI.create(userAction.getUri())))
                .flatMap(HttpSession::initialize)
                .flatMap(httpSession -> httpSession.select(path));
    }

    private String pathFromUri(String uri) {
        String path = "";
        if(uri.startsWith(ODSConstants.HTTPS_URI_SCHEME) || uri.startsWith(ODSConstants.HTTP_URI_SCHEME))
            path = uri;

        try {
            path = java.net.URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return path;
    }

    @Override
    public Mono<Stat> list(String cookie, UserAction userAction) {
        return getResourceWithUserActionUri(cookie, userAction).flatMap(HttpResource::stat);
    }

    @Override
    /* Not allowed */
    public Mono<Boolean> mkdir(String cookie, UserAction userAction) {
        throw new UnsupportedOperationException();
    }

    @Override
    /* Not allowed */
    public Mono<Boolean> delete(String cookie, UserAction userAction) {
        throw new UnsupportedOperationException();
    }


    @Override
    public Mono<String> download(String cookie, UserAction userAction) {
        return null;
    }
}
