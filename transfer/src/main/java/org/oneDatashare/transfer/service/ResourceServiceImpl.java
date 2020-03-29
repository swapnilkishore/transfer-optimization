package org.oneDatashare.transfer.service;

import com.oneDatashare.transfer.model.core.*;

import org.oneDatashare.transfer.model.core.*;
import org.onedatashare.module.globusapi.GlobusClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.UUID;


public class ResourceServiceImpl {
    /**
     * Make it the return type Mono<Job> before implementing
     * @param receivedJob
     * @return
     */
    public void submit(Job receivedJob) {
        processTransferFromJob(receivedJob);
    }


    /**
     * We need to change at lot in below function, Mainly the credential part as it will come from ashish.
     * Thats why i have not imported related files for this class
     * @param job
     */
    public void processTransferFromJob(Job job) {
        Transfer<Resource, Resource> transfer = new Transfer<>();
        Disposable ongoingJob = getResourceWithUserActionResource(user.get(), job.getSrc())
                .map(transfer::setSource)
                .flatMap(t -> getResourceWithUserActionResource(user.get(), job.getDest()))
                .map(transfer::setDestination)
                .flux()
                .flatMap(transfer1 -> {
                    return transfer1.start(ODSConstants.TRANSFER_SLICE_SIZE);
                })
                .doOnSubscribe(s -> {
                    job.setStatus(JobStatus.transferring);
                })
                .map(tf -> {
                    return job.updateJobWithTransferInfo(tf);
                })
                .flatMap(jobService::saveJob)
                .doOnCancel(new RunnableCanceler(job))
                .doFinally(s -> {
                    if (job.getStatus() != JobStatus.cancelled && job.getStatus() != JobStatus.failed)
                        job.setStatus(JobStatus.complete);
                    jobService.saveJob(job).subscribe();
                    ongoingJobs.remove(job.getUuid());
                })
                .subscribe();
        ongoingJobs.put(job.getUuid(), ongoingJob);
    }
    public Mono<Resource> getResourceWithUserActionResource(User userObj, UserActionResource userActionResource) {
        final String path = pathFromUri(userActionResource.getUri());
        String id = userActionResource.getId();
        ArrayList<IdMap> idMap = userActionResource.getMap();
        return Mono.just(userObj)
                .flatMap(user -> createCredential(userActionResource, user))
                .map(credential -> createSession(userActionResource.getUri(), credential))
                .flatMap(session -> {
                    if (session instanceof GoogleDriveSession && !userActionResource.getCredential().isTokenSaved())
                        return ((GoogleDriveSession) session).initializeNotSaved();
                    if (session instanceof BoxSession && !userActionResource.getCredential().isTokenSaved())
                        return ((BoxSession) session).initializeNotSaved();
                    else
                        return session.initialize();
                })
                .flatMap(session -> ((Session) session).select(path, id, idMap));
    }
    public Mono<Credential> createCredential(UserActionResource userActionResource, User user) {
        if (userActionResource.getUri().startsWith(ODSConstants.DROPBOX_URI_SCHEME) ||
                userActionResource.getUri().startsWith(ODSConstants.DRIVE_URI_SCHEME) || userActionResource.getUri().startsWith(ODSConstants.BOX_URI_SCHEME)) {
            if (user.isSaveOAuthTokens()) {
                return Mono.just(
                        user.getCredentials().get(
                                UUID.fromString(userActionResource.getCredential()
                                        .getUuid())
                        ));
            }
            else {
                return Mono.just( new OAuthCredential(userActionResource.getCredential().getToken()));
            }
        }
        else if (userActionResource.getUri().equals(ODSConstants.UPLOAD_IDENTIFIER)) {
            return Mono.just( userActionResource.getUploader() );
        }
        else if (userActionResource.getUri().startsWith(ODSConstants.GRIDFTP_URI_SCHEME)) {
            GlobusClient gc = userService.getGlobusClientFromUser(user);
            return Mono.just(new GlobusWebClientCredential(userActionResource.getCredential().getGlobusEndpoint(), gc));
        }
        else if (userActionResource.getUri().startsWith(ODSConstants.SFTP_URI_SCHEME) ||
                userActionResource.getUri().startsWith(ODSConstants.SCP_URI_SCHEME)){
            return decryptionService.getDecryptedCredential(userActionResource.getCredential())
                    .map(cred -> new UserInfoCredential(cred));
        }
        else
            return Mono.just(new UserInfoCredential(userActionResource.getCredential()));
    }
}
