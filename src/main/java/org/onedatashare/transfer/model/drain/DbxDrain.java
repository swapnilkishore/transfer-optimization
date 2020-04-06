package org.onedatashare.transfer.model.drain;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.CommitInfo;
import com.dropbox.core.v2.files.UploadSessionCursor;
import com.dropbox.core.v2.files.WriteMode;
import org.onedatashare.transfer.model.core.Slice;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;

public class DbxDrain implements Drain {
    String drainPath ;//= getPath();
    long uploaded = 0L;
    InputStream in = new ByteArrayInputStream(new byte[]{});
    String sessionId;
    UploadSessionCursor cursor;

    public DbxDrain start(String drainPath){
        this.drainPath = drainPath;
        return start();
    }

    public DbxDrain start() {
        try {
//            sessionId = getSession().getClient().files().uploadSessionStart()
//                    .uploadAndFinish(in, 0L)
//                    .getSessionId();
            cursor = new UploadSessionCursor(sessionId, uploaded);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public void drain(Slice slice) {
        InputStream sliceInputStream = new ByteArrayInputStream(slice.asBytes());
//        try {
//            getSession().getClient().files().uploadSessionAppendV2(cursor)
//                    .uploadAndFinish(sliceInputStream, slice.length());
//        } catch (DbxException | IOException e) {
//            e.printStackTrace();
//        }
        uploaded += slice.length();
        cursor = new UploadSessionCursor(sessionId, uploaded);
    }

    public void finish() {
        CommitInfo commitInfo = CommitInfo.newBuilder(drainPath)
                .withMode(WriteMode.ADD)
                .withClientModified(new Date())
                .build();
//        try {
//            getSession().getClient().files().uploadSessionFinish(cursor, commitInfo)
//                    .uploadAndFinish(in, 0L);
//        } catch (DbxException | IOException e) {
//            e.printStackTrace();
//        }
    }
}

