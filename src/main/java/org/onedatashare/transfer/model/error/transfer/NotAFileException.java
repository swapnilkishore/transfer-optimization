package org.onedatashare.transfer.model.error.transfer;

public class NotAFileException extends Exception{
    public NotAFileException(){
        super("Not a file");
    }
}
