package org.oneDatashare.transfer.module.gridftp;

import org.oneDatashare.transfer.model.core.Credential;
import org.oneDatashare.transfer.model.core.Session;
import org.oneDatashare.transfer.model.credential.GlobusWebClientCredential;
import org.oneDatashare.transfer.model.useraction.IdMap;
import org.onedatashare.module.globusapi.EndPoint;
import org.onedatashare.module.globusapi.GlobusClient;
import org.oneDatashare.transfer.model.error.AuthenticationRequired;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;

public class GridftpSession extends Session<GridftpSession, GridftpResource> {
    public GlobusClient client;
    EndPoint endpoint;
    public GridftpSession(URI uri, Credential cred) {
        super(uri, cred);
    }

    @Override
    public Mono<GridftpResource> select(String path) {
        return Mono.just(new GridftpResource(this, path));
    }

    @Override
    public Mono<GridftpResource> select(String path, String id) {
        return Mono.just(new GridftpResource(this, path));
    }

    @Override
    public Mono<GridftpResource> select(String path, String id, ArrayList<IdMap> idMap) {
        return Mono.just(new GridftpResource(this, path));
    }

    @Override
    public Mono<GridftpSession> initialize() {
        return Mono.create(s -> {
            if(getCredential() instanceof GlobusWebClientCredential){
                client = ((GlobusWebClientCredential) getCredential())._globusClient;
                endpoint = ((GlobusWebClientCredential) getCredential())._endpoint;
                s.success(this);
            }
            else s.error(new AuthenticationRequired("gridftp"));
        });
    }
}
