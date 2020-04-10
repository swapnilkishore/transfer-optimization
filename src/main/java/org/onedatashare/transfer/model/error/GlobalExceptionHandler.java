package org.onedatashare.transfer.model.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * This Class provides a deafault method of handling exceptions throughout the project
 * */
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handle(NotFoundException nfException){
//        ODSLoggerService.logError(nfException.toString());
        return new ResponseEntity<>(nfException.getMessage(), nfException.status);
    }

}
