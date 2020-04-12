package org.onedatashare.transfer.controller;

import org.onedatashare.transfer.model.request.TransferJobRequest;
import org.onedatashare.transfer.service.TransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Contoller for handling file/folder transfer requests
 */
@RestController
@RequestMapping("/api/transfer")
public class TransferController {

    @Autowired
    private TransferService resourceService;

    /**
     * Handler for POST requests of transfers
     * @param request - Request data with transfer information
     * @return Mono\<Job\>
     */
    @PostMapping
    public Mono<Void> start(@RequestBody TransferJobRequest request) {
        return resourceService.submit(request);
    }
}