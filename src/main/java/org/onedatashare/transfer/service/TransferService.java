package org.onedatashare.transfer.service;

import lombok.SneakyThrows;
import org.onedatashare.transfer.model.core.*;
import org.onedatashare.transfer.model.credential.EndpointCredential;
import org.onedatashare.transfer.model.request.TransferJobRequest;
import org.onedatashare.transfer.module.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.onedatashare.transfer.model.core.ODSConstants.*;
import static org.onedatashare.transfer.model.credential.CredentialConstants.*;

@Service
public class TransferService {
    @Autowired
    private CredentialService credentialService;

    private ConcurrentHashMap<UUID, Disposable> ongoingJobs = new ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(TransferService.class);

    public Mono<? extends EndpointCredential> getEndpointCredential(String token, EndpointType type, String credId){
        if(ACCOUNT_CRED_TYPE.contains(type)){
            return credentialService.fetchAccountCredential(token, type, credId);
        }
        else if(OAUTH_CRED_TYPE.contains(type)){
            return credentialService.fetchOAuthCredential(token, type, credId);
        }
        return Mono.error(new Exception("Invalid endpoint type. Must either be AccountCred or OauthCred type"));
    }

    @SneakyThrows
    public Resource createResource(EndpointCredential cred, EndpointType type){
        switch (type){
            case http:
                return new HttpResource(cred);
            case box:
                return new BoxResource(cred);
            case ftp:
                return new FtpResource(cred);
            case dropbox:
                return new DropboxResource(cred);
            case gdrive:
                return new GDriveResource(cred);
            case sftp:
                return new SftpResource(cred);
            default:
                return null;
        }
    }

    private Mono<String> getUserCredFromRequest(){
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (String) securityContext.getAuthentication().getCredentials());
    }

    public Mono<Void> submit(TransferJobRequest request){
        return getUserCredFromRequest()
                .flatMap(token -> {
                    TransferJobRequest.Source source = request.getSource();
                    TransferJobRequest.Destination destination = request.getDestination();
                    Mono<Resource> sourceResourceMono = getEndpointCredential(token, source.getType(), source.getCredId())
                            .map(credential -> createResource(credential, source.getType()));
                    Mono<Resource> destResourceMono =getEndpointCredential(token, destination.getType(), destination.getCredId())
                            .map(credential -> createResource(credential, destination.getType()));
                    return Mono.zip(sourceResourceMono, destResourceMono, Transfer::new);
                })
                .map(transfer -> {
                    TransferJobRequest.Source source = request.getSource();
                    ArrayList<IdMap> filesToTransfer = new ArrayList<>(source.getUriList().length);
                    for(int i = 0; i < source.getUriList().length; i++){
                        IdMap tempFile = new IdMap();
                        if(source.getIdList() != null) {
                            tempFile.setId(source.getIdList()[i]);
                        }
                        tempFile.setUri(source.getUriList()[i]);
                        filesToTransfer.add(tempFile);
                    }
                    transfer.setFilesToTransfer(filesToTransfer);
                    return transfer.blockingStart(TRANSFER_SLICE_SIZE);
                })
                .subscribeOn(Schedulers.elastic())
                .then();
    }

    /**
     * This method cancel an ongoing transfer.
     * User email and job id passed in the request is used to obtain the job UUID,
     * which is in turn used to access the ongoing job flux from the ongoingJobs map.
     * This flux is then disposed and the job is evicted from the map to cancel the transfer.
     *
     * @param uuid
     * @return Mono of job that was stopped
     */
    public Mono<Void> cancel(UUID uuid) {
        return Mono.error(new Exception("Unsupported operation"));
    }


}
