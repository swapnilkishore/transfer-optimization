package org.onedatashare.transfer.model.drain;

import com.box.sdk.*;
import org.onedatashare.transfer.model.core.Slice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * @README
 * @Author Javier Falca
 * Box Chunked Upload has some cryptic properties that are not too well documented
 * 1.) To perform a chunked upload, a file must be greater than 20MB, any less will have to be
 * sent as a single chunk with a different function.
 * 2.) Each chunk must be exactly 8MB in size, if this number is not met, the chunk will fail to upload.
 * 3.) A SHA-1 Base64 hash of the entire file must be provided at the end of the finish state during the commit.
 */
public final class BoxDrain implements Drain {
    private String name;
    private String id;
    private BoxAPIConnection connection;

    private ByteArrayOutputStream chunk = new ByteArrayOutputStream();
    private long size = 0, sizeUploaded = 0;
    private int part_size;

    private BoxFileUploadSession.Info sessionInfo;
    private BoxFileUploadSession session;

    private MessageDigest sha1;
    private List<BoxFileUploadSessionPart> parts;
    private ByteArrayInputStream smallFileStream;
    private boolean isSmall;

    private static final long THRESHOLD = 20971520;

    private BoxDrain(){}

    private static BoxDrain getInstance(String fileName, long size) throws Exception{
        BoxDrain boxDrain = new BoxDrain();
        boxDrain.isSmall = (size < THRESHOLD) ? true : false;
        boxDrain.size = size;
        boxDrain.sha1 = MessageDigest.getInstance("SHA-1");
        boxDrain.name = fileName;
        BoxFolder folder = new BoxFolder(boxDrain.connection, boxDrain.id);
        if(!boxDrain.isSmall) {
            boxDrain.sessionInfo = folder.createUploadSession(boxDrain.name, boxDrain.size);
            boxDrain.parts = new ArrayList<>();
            boxDrain.session = boxDrain.sessionInfo.getResource();
        }
        return null;
    }


    @Override
    public void drain(Slice slice) {
        try {
            //Box only allows chunked upload for files greater than 20MB at 8MB chunks
            if (isSmall) {
                chunk.write(slice.asBytes());
            } else {
                try {
                    part_size = sessionInfo.getPartSize();
                } catch(NullPointerException npe){

                }
                chunk.write(slice.asBytes());
                if (chunk.size() == part_size) {
                    try {
                        BoxFileUploadSessionPart part = session.uploadPart(chunk.toByteArray(), sizeUploaded, chunk.size(), size);
                        parts.add(part);
                        sizeUploaded = sizeUploaded + chunk.size();
                        sha1.update(chunk.toByteArray());
                        chunk = new ByteArrayOutputStream();
                    }catch(Exception e){
                        //Part already exists
                    }
                }

            }
        }catch(Exception e){
            e.printStackTrace();
        }

    }


    @Override
    public void finish() throws Exception{
        if (isSmall) {
            BoxFolder folder = new BoxFolder(connection, id);
            smallFileStream = new ByteArrayInputStream(chunk.toByteArray());
            BoxFile.Info smallFile = folder.uploadFile(smallFileStream, name);
            smallFileStream.close();
        } else {
            if (chunk.size() > 0) {
                BoxFileUploadSessionPart part = session.uploadPart(chunk.toByteArray(), sizeUploaded, chunk.size(), size);
                parts.add(part);
                sizeUploaded = sizeUploaded + chunk.size();
                sha1.update(chunk.toByteArray());
            }

            byte[] digestBytes = sha1.digest();
            //Base64 encoding of the hash
            String digestStr = Base64.getEncoder().encodeToString(digestBytes);
            BoxFile.Info largeFile = session.commit(digestStr, parts, null, null, null);
        }
    }
}
