package org.onedatashare.transfer.model.error;

public class ForbiddenAction extends ODSError {

    public ForbiddenAction( String err){
        super(err);
        type = "Forbidden Action";
        error = err;
    }

}
