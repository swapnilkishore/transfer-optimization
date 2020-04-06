package org.onedatashare.transfer.controller;

import org.onedatashare.transfer.model.core.ODSConstants;
import org.onedatashare.transfer.model.request.JobRequestData;
import org.onedatashare.transfer.model.request.TransferRequest;
import org.onedatashare.transfer.model.useraction.UserAction;
import org.onedatashare.transfer.service.ResourceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * Contoller for handling file/folder transfer requests
 */
@RestController
@RequestMapping("/api/transfer/submit")
public class SubmitController {

    @Autowired
    private ResourceServiceImpl resourceService;

    /**
     * Handler for POST requests of transfers
     * @param headers - Incoming request headers
     * @param transferRequest - Request data with transfer information
     * @return Mono\<Job\>
     */
    @PostMapping
    public Object submit(@RequestHeader HttpHeaders headers, @RequestBody TransferRequest transferRequest) {
        String cookie = headers.getFirst(ODSConstants.COOKIE);
        UserAction userAction = UserAction.convertToUserAction(transferRequest);
        return resourceService.submit(cookie, userAction);
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