package org.onedatashare.transfer.controller;

import org.onedatashare.transfer.model.core.ODSConstants;
import org.onedatashare.transfer.model.request.TransferRequest;
import org.onedatashare.transfer.model.useraction.UserAction;
import org.onedatashare.transfer.service.TransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * Contoller for handling file/folder transfer requests
 */
@RestController
@RequestMapping("/api/transfer")
public class SubmitController {

    @Autowired
    private TransferService resourceService;

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
}