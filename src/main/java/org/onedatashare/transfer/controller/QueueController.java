package org.onedatashare.transfer.controller;

import org.onedatashare.transfer.model.core.Job;
import org.onedatashare.transfer.model.core.JobDetails;
import org.onedatashare.transfer.model.core.ODSConstants;
import org.onedatashare.transfer.model.jobaction.JobRequest;
import org.onedatashare.transfer.model.request.JobRequestData;
import org.onedatashare.transfer.model.useraction.UserAction;
import org.onedatashare.transfer.service.JobService;
import org.onedatashare.transfer.service.TransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Controller for handling GET requests from queue page
 */
@RestController
@RequestMapping("/api/q")
public class QueueController {

    @Autowired
    private JobService jobService;

    @Autowired
    private TransferService resourceService;

    /**
     * Handler for queue GET requests
     * @param jobDetails - Request data needed for fetching Jobs
     * @return Mono\<JobDetails\>
     */
    //TODO: remove body
    @GetMapping
    public Mono<JobDetails> getJobsForUser(@RequestBody JobRequest jobDetails){
        return jobService.getJobsForUser(jobDetails);
    }

    //TODO: change the function to use query instead of using filter (might make it faster)
    //TODO: remove body
    @GetMapping("/update-user-jobs")
    public Mono<List<Job>> updateJobsForUser(@RequestBody List<UUID> jobIds) {
        return jobService.getUpdatesForUser(jobIds);
    }

    /**
     * Handler that invokes the service to cancel an ongoing job.
     *
     * @param headers - Incoming request headers
     * @param jobRequestData - Model containing the job ID of the transfer job to be stopped
     * @return Object - Mono of job that was stopped
     */
    @PostMapping("/cancel")
    public Object cancel(@RequestHeader HttpHeaders headers, @RequestBody JobRequestData jobRequestData) {
        String cookie = headers.getFirst(ODSConstants.COOKIE);
        UserAction userAction = UserAction.convertToUserAction(jobRequestData);
        return resourceService.cancel(cookie, userAction);
    }
}
