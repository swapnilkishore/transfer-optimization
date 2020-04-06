package org.onedatashare.transfer.model.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * This Class provides a deafault method of handling exceptions throughout the project
 * */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Exception handler for Invalid Login
     * @param ilException : Invalid ODS Credential Exception Object
     * */
    @ExceptionHandler(InvalidODSCredentialsException.class)
    public ResponseEntity<String> handle(InvalidODSCredentialsException ilException) {
//        ODSLoggerService.logError(ilException.toString());
        return new ResponseEntity<>(ilException.toString(), ilException.status);
    }


    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handle(NotFoundException nfException){
//        ODSLoggerService.logError(nfException.toString());
        return new ResponseEntity<>(nfException.getMessage(), nfException.status);
    }

}
