package org.onedatashare.transfer.service;

import org.onedatashare.transfer.model.core.*;
import org.onedatashare.transfer.model.credential.OAuthCredential;
import org.onedatashare.transfer.model.credential.UserInfoCredential;
import org.onedatashare.transfer.model.useraction.IdMap;
import org.onedatashare.transfer.model.useraction.UserAction;
import org.onedatashare.transfer.model.useraction.UserActionResource;
import org.onedatashare.transfer.module.box.BoxSession;
import org.onedatashare.transfer.module.dropbox.DbxSession;
import org.onedatashare.transfer.module.googledrive.GoogleDriveSession;
import org.onedatashare.transfer.module.http.HttpSession;
import org.onedatashare.transfer.module.vfs.VfsSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.onedatashare.transfer.model.core.ODSConstants.*;

@Service
public class TransferService {
    @Autowired
    private UserService userService;

    @Autowired
    private JobService jobService;

    private ConcurrentHashMap<UUID, Disposable> ongoingJobs = new ConcurrentHashMap<>();

    public Mono<Resource> getResourceWithUserActionResource(User userObj, UserActionResource userActionResource) {
        final String path = pathFromUri(userActionResource.getUri());
        String id = userActionResource.getId();
        ArrayList<IdMap> idMap = userActionResource.getMap();
        return Mono.just(userObj)
                .flatMap(user -> createCredential(userActionResource, user))
                .map(credential -> createSession(userActionResource.getUri(), credential))
                .flatMap(session -> session.initialize())
                .flatMap(session -> ((Session) session).select(path, id, idMap));
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

    public Mono<Credential> createCredential(UserActionResource userActionResource, User user) {
        if (userActionResource.getUri().startsWith(DROPBOX_URI_SCHEME) ||
                userActionResource.getUri().startsWith(DRIVE_URI_SCHEME) || userActionResource.getUri().startsWith(BOX_URI_SCHEME)) {
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
        else
            return Mono.just(new UserInfoCredential(userActionResource.getCredential()));
    }


    public Session createSession(String uri, Credential credential) {
        if (uri.startsWith(DROPBOX_URI_SCHEME)) {
            return new DbxSession(URI.create(uri), credential);
        } else if (uri.startsWith(DRIVE_URI_SCHEME))
            return new GoogleDriveSession(URI.create(uri), credential);
        else if(uri.startsWith(ODSConstants.BOX_URI_SCHEME)) {
            return new BoxSession(URI.create(uri), credential);
        }
        else if (uri.startsWith(HTTPS_URI_SCHEME) || uri.startsWith(HTTP_URI_SCHEME)) {
            return new HttpSession(URI.create(uri));
        }
        else {
            return new VfsSession(URI.create(uri), credential);
        }
    }

    public Mono<Job> submit(UserAction userAction) {
        AtomicReference<User> u = new AtomicReference<>();
        return userService.getLoggedInUser()
                .map(user -> {
                    Job job = new Job(userAction.getSrc(), userAction.getDest());
                    job.setStatus(JobStatus.scheduled);
                    job = user.saveJob(job);
                    userService.saveUser(user).subscribe();
                    u.set(user);
                    return job;
                })
                .flatMap(jobService::saveJob)
                .doOnSuccess(job -> processTransferFromJob(job, u))
                .subscribeOn(Schedulers.elastic());
    }

    /**
     * This method cancel an ongoing transfer.
     * User email and job id passed in the request is used to obtain the job UUID,
     * which is in turn used to access the ongoing job flux from the ongoingJobs map.
     * This flux is then disposed and the job is evicted from the map to cancel the transfer.
     *
     * @param cookie
     * @param userAction
     * @return Mono of job that was stopped
     */
    public Mono<Job> cancel(String cookie, UserAction userAction) {
        return userService.getLoggedInUser()
                .flatMap((User user) -> jobService.findJobByJobId(cookie, userAction.getJob_id())
                        .map(job -> {
                            try {
                                ongoingJobs.get(job.getUuid()).dispose();
                                ongoingJobs.remove(job.getUuid());
                            }catch (Exception e){
//                                ODSLoggerService.logError("Unable to remove job " + job.getUuid() + "- Not found");
                            }
                            return job.setStatus(JobStatus.cancelled);
                        }))
                .flatMap(jobService::saveJob);
    }

    public void processTransferFromJob(Job job, AtomicReference<User> user) {
        Transfer<Resource, Resource> transfer = new Transfer<>();
        Disposable ongoingJob = getResourceWithUserActionResource(user.get(), job.getSrc())
                .map(transfer::setSource)
                .flatMap(t -> getResourceWithUserActionResource(user.get(), job.getDest()))
                .map(transfer::setDestination)
                .flux()
                .flatMap(transfer1 -> transfer1.start(TRANSFER_SLICE_SIZE))
                .doOnSubscribe(s -> job.setStatus(JobStatus.transferring))
                .doOnCancel(new RunnableCanceler(job))
                .doFinally(s -> {
                    if (job.getStatus() != JobStatus.cancelled && job.getStatus() != JobStatus.failed)
                        job.setStatus(JobStatus.complete);
                    jobService.saveJob(job).subscribe();
                    ongoingJobs.remove(job.getUuid());
                })
                .map(job::updateJobWithTransferInfo)
                .flatMap(jobService::saveJob)
                .subscribe();
        ongoingJobs.put(job.getUuid(), ongoingJob);
    }

    class RunnableCanceler implements Runnable {
        Job job;

        public RunnableCanceler(Job job) {
            this.job = job;
        }

        @Override
        public void run() {
            job.setStatus(JobStatus.failed);
        }
    }
}
