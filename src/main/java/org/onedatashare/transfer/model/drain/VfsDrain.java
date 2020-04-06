package org.onedatashare.transfer.model.drain;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.onedatashare.transfer.model.core.Slice;

import java.io.IOException;
import java.io.OutputStream;

public class VfsDrain implements Drain {
    OutputStream outputStream;
    FileObject drainFileObject ;//= fileObject;

    @Override
    public VfsDrain start() {
        try {
            drainFileObject.createFile();
            outputStream = drainFileObject.getContent().getOutputStream();
        } catch (FileSystemException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public VfsDrain start(String drainPath) {
//        try {
//            drainFileObject = getSession().fileSystemManager.resolveFile(
//                    drainPath.substring(0, drainPath.lastIndexOf('/')), getSession().fileSystemOptions);
//            drainFileObject.createFolder();    // creates the folders for folder transfer
//            drainFileObject = getSession().fileSystemManager.resolveFile(drainPath, getSession().fileSystemOptions);
//            return start();
//        }
//        catch(FileSystemException fse){
////                ODSLoggerService.logError("Exception encountered while creating file object", fse);
//        }
        return null;
    }

    @Override
    public void drain(Slice slice) {
        try {
            outputStream.write(slice.asBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void finish() {
        try {
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
