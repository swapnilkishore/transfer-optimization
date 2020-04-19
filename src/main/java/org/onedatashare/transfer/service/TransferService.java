package org.onedatashare.transfer.service;

import lombok.SneakyThrows;
import org.onedatashare.transfer.model.TransferDetails;
import org.onedatashare.transfer.model.TransferDetailsRepository;
import org.onedatashare.transfer.model.core.*;
import org.onedatashare.transfer.model.credential.EndpointCredential;
import org.onedatashare.transfer.model.request.TransferJobRequest;
import org.onedatashare.transfer.module.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
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

    @Autowired
    private static TransferDetailsRepository repository;

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
                .map(s -> {
                    Authentication authentication = s.getAuthentication();
                    return (String) authentication.getCredentials();
                });
    }

    private ArrayList<IdMap> getFilesToTransfer(TransferJobRequest.Source source){
        ArrayList<IdMap> filesToTransfer = new ArrayList<>(source.getUrlList().length);
        for(int i = 0; i < source.getUrlList().length; i++){
            IdMap tempFile = new IdMap();
            if(source.getIdList() != null) {
                tempFile.setId(source.getIdList()[i]);
            }
            tempFile.setUri(source.getUrlList()[i]);
            filesToTransfer.add(tempFile);
        }
        return filesToTransfer;
    }

    public Mono<Void> submit(TransferJobRequest request){
        logger.info("In submit Function");
        //TransferDetails details = new TransferDetails("test", 2l);
       // repository.saveAll(Flux.just(details)).subscribe();
        return getUserCredFromRequest()
            .flatMap(token -> {
                TransferJobRequest.Source source = request.getSource();
                TransferJobRequest.Destination destination = request.getDestination();
                Mono<Resource> sourceResourceMono = getEndpointCredential(token, source.getType(), source.getCredId())
                        .map(credential -> createResource(credential, source.getType()));
                Mono<Resource> destResourceMono = getEndpointCredential(token, destination.getType(), destination.getCredId())
                        .map(credential -> createResource(credential, destination.getType()));
                return sourceResourceMono.zipWith(destResourceMono, Transfer::new);
            })
            .doOnNext(transfer -> {
                transfer.setFilesToTransfer(getFilesToTransfer(request.getSource()));
                transfer.setDestinationBaseUri(request.getDestination().getBaseUrl());
                transfer.setSourceBaseUri(request.getSource().getBaseUrl());
                transfer.start(TRANSFER_SLICE_SIZE).subscribe();
            })
            .doOnSubscribe(s -> logger.info("Transfer submit initiated"))
            .subscribeOn(Schedulers.elastic())
            .then();
    }

    public static void savetoMongo()
    {
        logger.info("in savetomongo");
        TransferDetails details = new TransferDetails("testfile", 2l);
        repository.saveAll(Flux.just(details)).subscribe();
        return;
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
