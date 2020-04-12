package org.onedatashare.transfer.module;

import org.apache.commons.vfs2.*;
import org.onedatashare.transfer.model.core.IdMap;
import org.onedatashare.transfer.model.credential.EndpointCredential;
import org.onedatashare.transfer.model.drain.Drain;
import org.onedatashare.transfer.model.drain.VfsDrain;
import org.onedatashare.transfer.model.tap.Tap;
import org.onedatashare.transfer.model.tap.VfsTap;

public class VfsResource extends Resource {
    protected FileSystemManager fileSystemManager;
    protected FileSystemOptions fileSystemOptions;

    VfsResource(EndpointCredential credential){
        this.credential = credential;
    }

    @Override
    public Tap getTap(IdMap idMap){
        try {
            FileObject fileObject = fileSystemManager.resolveFile(idMap.getUri());
            FileContent content = fileObject.getContent();
            long size = content.getSize();
            return VfsTap.initialize(content.getInputStream(), size);
        } catch (FileSystemException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Drain getDrain(IdMap idMap){
        try {
            FileObject fileObject = fileSystemManager.resolveFile(idMap.getUri(), fileSystemOptions);
            return VfsDrain.initialize(fileObject.getContent().getOutputStream());
        } catch (FileSystemException e) {
            e.printStackTrace();
        }
        return null;
    }
}
