package org.onedatashare.transfer.resource;

import org.apache.commons.vfs2.*;
import org.onedatashare.transfer.model.core.IdMap;
import org.onedatashare.transfer.model.credential.EndpointCredential;
import org.onedatashare.transfer.model.drain.Drain;
import org.onedatashare.transfer.model.drain.VfsDrain;
import org.onedatashare.transfer.model.error.transfer.NotAFileException;
import org.onedatashare.transfer.model.tap.Tap;
import org.onedatashare.transfer.model.tap.VfsTap;

public class VfsResource extends Resource {
    protected FileSystemManager fileSystemManager;
    protected FileSystemOptions fileSystemOptions;

    VfsResource(EndpointCredential credential){
        this.credential = credential;
    }


    @Override
    public Tap getTap(IdMap idMap, String baseUrl) throws Exception{
        FileObject fileObject = fileSystemManager.resolveFile(baseUrl + idMap.getUri());
        if(fileObject.isFile() != true){
            throw new NotAFileException();
        }
        FileContent content = fileObject.getContent();
        long size = content.getSize();
        return VfsTap.initialize(content.getInputStream(), size);
    }

    @Override
    public Drain getDrain(IdMap idMap, String baseUrl) throws Exception {
        FileObject fileObject = fileSystemManager.resolveFile(baseUrl + idMap.getUri(), fileSystemOptions);
        //Creates the required folders and file
        fileObject.createFile();
        return VfsDrain.initialize(fileObject.getContent().getOutputStream());
    }
}
