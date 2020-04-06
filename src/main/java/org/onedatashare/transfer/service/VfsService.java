package org.onedatashare.transfer.service;

import org.onedatashare.transfer.model.core.*;
import org.onedatashare.transfer.model.credential.UserInfoCredential;
import org.onedatashare.transfer.model.useraction.UserAction;
import org.onedatashare.transfer.model.useraction.UserActionResource;
import org.onedatashare.transfer.module.vfs.VfsResource;
import org.onedatashare.transfer.module.vfs.VfsSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import static org.onedatashare.transfer.model.core.ODSConstants.TRANSFER_SLICE_SIZE;

@Service
public class VfsService extends ResourceService {
    @Autowired
    private UserService userService;

    @Autowired
    private JobService jobService;


    public Mono<VfsResource> getResourceWithUserActionUri(String cookie, UserAction userAction) {
        final String path = pathFromUri(userAction.getUri());
        return userService.getLoggedInUser(cookie)
                .flatMap(user -> {
                        // Credentials for FTP will be null
                        return Mono.just(new UserInfoCredential(null));
                })
                .map(credential -> {
                    // Encoding the resource URI to avoid errors due to spaces in file/directory names
                    String encodedURI = userAction.getUri();

                    try {
                        encodedURI = URLEncoder.encode(userAction.getUri(), "UTF-8");
                    }
                    catch(UnsupportedEncodingException uee){
//                        ODSLoggerService.logError("Exception encountered while encoding input URI");
                        Mono.error(uee);
                    }
                    return new VfsSession(URI.create(encodedURI), credential);
                })
                .flatMap(vfsSession -> vfsSession.initialize())
                .flatMap(vfsSession -> vfsSession.select(path, userAction.getPortNumber()));
    }

    public Mono<VfsResource> getResourceWithUserActionResource(String cookie, UserActionResource userActionResource) {
        final String path = pathFromUri(userActionResource.getUri());
        return userService.getLoggedInUser(cookie)
                .flatMap(user -> Mono.just(new UserInfoCredential(null)))
                .map(credential -> {
                    // Encoding the resource URI to avoid errors due to spaces in file/directory names
                    String encodedURI = userActionResource.getUri();
                    try {
                        encodedURI = URLEncoder.encode(userActionResource.getUri(), "UTF-8");
                    }
                    catch(UnsupportedEncodingException uee){
//                        ODSLoggerService.logError("Exception encountered while encoding input URI");
                        Mono.error(uee);
                    }
                    return new VfsSession(URI.create(encodedURI), credential);
                })
                .flatMap(VfsSession::initialize)
                .flatMap(vfsSession -> vfsSession.select(path));
    }


    public String pathFromUri(String uri) {
        String path = "";
        if (uri.contains(ODSConstants.DROPBOX_URI_SCHEME)) {
            path = uri.substring(ODSConstants.DROPBOX_URI_SCHEME.length() - 1);
        } else path = uri;
        try {
            path = java.net.URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return path;
    }

    public Mono<Stat> list(String cookie, UserAction userAction) {
        return getResourceWithUserActionUri(cookie, userAction).flatMap(VfsResource::stat);
    }

    public Mono<Boolean> mkdir(String cookie, UserAction userAction) {
        return getResourceWithUserActionUri(cookie, userAction)
                .flatMap(VfsResource::mkdir)
                .map(r -> true);
    }

    public Mono<Boolean> delete(String cookie, UserAction userAction) {
        return getResourceWithUserActionUri(cookie, userAction)
                .flatMap(VfsResource::delete)
                .map(val -> true);
    }

    public Mono<Job> submit(String cookie, UserAction userAction) {
        return userService.getLoggedInUser(cookie)
                .map(user -> {
                    Job job = new Job(userAction.getSrc(), userAction.getDest());
                    job.setStatus(JobStatus.scheduled);
                    job = user.saveJob(job);
                    userService.saveUser(user).subscribe();
                    return job;
                })
                .flatMap(jobService::saveJob)
                .doOnSuccess(job -> processTransferFromJob(job, cookie))
                .subscribeOn(Schedulers.elastic());
    }

    @Override
    public Mono<String> download(String cookie, UserAction userAction) {
        return getResourceWithUserActionUri(cookie, userAction).flatMap(VfsResource::getDownloadURL);
    }

    public void processTransferFromJob(Job job, String cookie) {
        Transfer<Resource, Resource> transfer = new Transfer<>();
        getResourceWithUserActionResource(cookie, job.getSrc())
                .map(transfer::setSource)
                .flatMap(t -> getResourceWithUserActionResource(cookie, job.getDest()))
                .map(transfer::setDestination)
                .flux()
                .flatMap(transfer1 -> transfer1.start(TRANSFER_SLICE_SIZE))
                .doOnSubscribe(s -> job.setStatus(JobStatus.transferring))
                .doFinally(s -> {
                    job.setStatus(JobStatus.complete);
                    jobService.saveJob(job).subscribe();
                })
                .map(job::updateJobWithTransferInfo)
                .flatMap(jobService::saveJob)
                .subscribe();
    }

    public Mono<String> getDownloadURL(String cookie, UserAction userAction) {
        return getResourceWithUserActionUri(cookie, userAction).flatMap(VfsResource::getDownloadURL);
    }

    public Mono<ResponseEntity> getSftpDownloadStream(String cookie, UserActionResource userActionResource) {
        return getResourceWithUserActionResource(cookie, userActionResource).flatMap(VfsResource::sftpObject);
    }
}
