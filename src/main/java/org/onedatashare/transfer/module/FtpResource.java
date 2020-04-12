package org.onedatashare.transfer.module;

import lombok.SneakyThrows;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder;
import org.onedatashare.transfer.model.core.IdMap;
import org.onedatashare.transfer.model.credential.AccountEndpointCredential;
import org.onedatashare.transfer.model.credential.EndpointCredential;
import org.onedatashare.transfer.model.tap.Tap;
import org.onedatashare.transfer.model.tap.VfsTap;

public class FtpResource extends Resource {
    private FileSystemManager fileSystemManager;
    private FileSystemOptions fileSystemOptions;

    @SneakyThrows
    public FtpResource(EndpointCredential credential) throws FileSystemException {
        super(credential);
        this.fileSystemOptions = new FileSystemOptions();
        FtpFileSystemConfigBuilder.getInstance().setPassiveMode(this.fileSystemOptions, true);

        AccountEndpointCredential accountCredential = (AccountEndpointCredential) credential;

        //Handling authentication
        if(accountCredential.getUsername() != null) {
            StaticUserAuthenticator auth = new StaticUserAuthenticator(accountCredential.getAccountId(), accountCredential.getUsername(), accountCredential.getSecret());
            DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(this.fileSystemOptions, auth);
        }

        this.fileSystemManager = VFS.getManager();
    }

    @Override
    public Tap getTap(IdMap idMap){
        try {
            FileObject fileObject = fileSystemManager.resolveFile(idMap.getUri());
            long size = fileObject.getContent().getSize();
            return new VfsTap(fileObject.getContent().getInputStream(), size);
        } catch (FileSystemException e) {
            e.printStackTrace();
        }
        return null;
    }

}
