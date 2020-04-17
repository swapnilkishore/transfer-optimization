package org.onedatashare.transfer.model.drain;

import com.dropbox.core.v2.files.CommitInfo;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.UploadSessionCursor;
import com.dropbox.core.v2.files.WriteMode;
import org.onedatashare.transfer.model.core.Slice;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;

public class DropboxDrain implements Drain {
    private String finalPath;
    private long uploaded;
    private InputStream in;
    private String sessionId;
    private UploadSessionCursor cursor;
    private DbxUserFilesRequests requests;

    private DropboxDrain(){}

    public static DropboxDrain initialize(String finalPath, DbxUserFilesRequests requests) throws Exception{
        DropboxDrain dropboxDrain = new DropboxDrain();
        dropboxDrain.finalPath = finalPath;
        dropboxDrain.requests = requests;
        dropboxDrain.in = new ByteArrayInputStream(new byte[]{});
        dropboxDrain.sessionId = requests.uploadSessionStart()
                .uploadAndFinish(dropboxDrain.in, 0L)
                .getSessionId();
        dropboxDrain.cursor = new UploadSessionCursor(dropboxDrain.sessionId, dropboxDrain.uploaded);
        return dropboxDrain;
    }

    public void drain(Slice slice) throws Exception{
        InputStream sliceInputStream = new ByteArrayInputStream(slice.asBytes());
        this.requests.uploadSessionAppendV2(this.cursor)
                .uploadAndFinish(sliceInputStream, slice.length());
        this.uploaded += slice.length();
        this.cursor = new UploadSessionCursor(this.sessionId, this.uploaded);
    }

    public void finish() throws Exception{
        CommitInfo commitInfo = CommitInfo.newBuilder(this.finalPath)
                .withMode(WriteMode.ADD)
                .withClientModified(new Date())
                .build();
        this.requests.uploadSessionFinish(this.cursor, commitInfo)
                .uploadAndFinish(in, 0L);
    }
}