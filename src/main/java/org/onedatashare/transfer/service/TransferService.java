package org.onedatashare.transfer.service;

import org.onedatashare.transfer.model.core.*;
import org.onedatashare.transfer.model.credential.EndpointCredential;
import org.onedatashare.transfer.model.request.TransferJobRequest;
import org.onedatashare.transfer.module.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.onedatashare.transfer.model.core.ODSConstants.*;
import static org.onedatashare.transfer.model.credential.CredentialConstants.*;

@Service
public class TransferService {
    @Autowired
    private CredentialService credentialService;

    private ConcurrentHashMap<UUID, Disposable> ongoingJobs = new ConcurrentHashMap<>();

    public Mono<? extends EndpointCredential> getEndpointCredential(String token, EndpointType type, String credId){
        if(ACCOUNT_CRED_TYPE.contains(type)){
            return credentialService.fetchAccountCredential(token, type, credId);
        }
        else if(OAUTH_CRED_TYPE.contains(type)){
            return credentialService.fetchOAuthCredential(token, type, credId);
        }
        return Mono.empty();
    }

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
                return new Resource();
        }
    }

    public String pathFromUri(String uri) {
        String path = "";
        if (uri.startsWith(DROPBOX_URI_SCHEME))
            path = uri.substring(DROPBOX_URI_SCHEME.length() - 1);
        else if (uri.startsWith(DRIVE_URI_SCHEME))
            path = uri.substring(DRIVE_URI_SCHEME.length() - 1);
        else
            path = uri;

        try {
            path = java.net.URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return path;
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
                .map(transfer -> transfer.start(TRANSFER_SLICE_SIZE))
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
    public Mono<Job> cancel(UUID uuid) {
        return null;
    }


    public void processTransferFromJob(TransferJob job) {
//        Transfer<ResourceOld, ResourceOld> transfer = new Transfer<>();

//        Disposable ongoingJob = getResourceWithUserActionResource(user.get(), job.getSrc())
//                .map(transfer::setSource)
//                .flatMap(t -> getResourceWithUserActionResource(user.get(), job.getDest()))
//                .map(transfer::setDestination)
//                .flux()
//                .flatMap(transfer1 -> transfer1.start(TRANSFER_SLICE_SIZE))
//                .doOnSubscribe(s -> job.setStatus(JobStatus.transferring))
//                .doOnCancel(new RunnableCanceler(job))
//                .doFinally(s -> {
//                    if (job.getStatus() != JobStatus.cancelled && job.getStatus() != JobStatus.failed)
//                        job.setStatus(JobStatus.complete);
//                    jobService.saveJob(job).subscribe();
//                    ongoingJobs.remove(job.getUuid());
//                })
//                .map(job::updateJobWithTransferInfo)
//                .flatMap(jobService::saveJob)
//                .subscribe();
//        ongoingJobs.put(job.getUuid(), ongoingJob);
    }

}
