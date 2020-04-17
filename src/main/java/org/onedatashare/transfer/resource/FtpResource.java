package org.onedatashare.transfer.resource;

import lombok.SneakyThrows;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder;
import org.onedatashare.transfer.model.credential.AccountEndpointCredential;
import org.onedatashare.transfer.model.credential.EndpointCredential;

public class FtpResource extends VfsResource {
    @SneakyThrows
    public FtpResource(EndpointCredential credential) {
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
}
