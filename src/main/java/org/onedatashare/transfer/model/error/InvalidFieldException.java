package org.onedatashare.transfer.model.error;

public class InvalidFieldException extends ODSError {

    public InvalidFieldException(String err){
        super(err);
        type = "Invalid Field";
        error = err;
    }
    
}
