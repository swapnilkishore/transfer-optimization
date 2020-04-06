package org.onedatashare.transfer.service;

import org.onedatashare.transfer.model.core.Credential;
import org.onedatashare.transfer.model.core.Stat;
import org.onedatashare.transfer.model.core.User;
import org.onedatashare.transfer.model.error.AuthenticationRequired;
import org.onedatashare.transfer.model.error.NotFoundException;
import org.onedatashare.transfer.model.useraction.UserAction;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;

import java.util.Map;
import java.util.UUID;

public abstract class ResourceService {
    public abstract Mono<Stat> list(String cookie, UserAction userAction);
    public abstract Mono<Boolean> mkdir(String cookie, UserAction userAction);
    public abstract Mono<Boolean> delete(String cookie, UserAction userAction);
    public abstract Mono<String> download(String cookie, UserAction userAction);

    protected void fetchCredentialsFromUserAction(User usr, SynchronousSink sink, UserAction userAction){
        if(userAction.getCredential() == null || userAction.getCredential().getUuid() == null) {
            sink.error(new AuthenticationRequired("oauth"));
        }
        Map credMap = usr.getCredentials();
        Credential credential = (Credential) credMap.get(UUID.fromString(userAction.getCredential().getUuid()));
        if(credential == null){
            sink.error(new NotFoundException("Credentials for the given UUID not found"));
        }
        sink.next(credential);
    }
}