package org.onedatashare.transfer.model.drain;

import com.box.sdk.*;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.google.common.primitives.Bytes;
import org.onedatashare.transfer.model.core.Slice;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
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
    private long size;
    private BoxFolder folder;
    private boolean isChunkedUpload = false;

    //For direct upload
    private ByteArrayBuilder byteArrayBuilder;

    //For chunked uploads
    private BoxFileUploadSession.Info sessionInfo;
    private BoxFileUploadSession session;
    private MessageDigest messageDigest;
    private List<BoxFileUploadSessionPart> parts;
    private int partSize;
    private long uploadedSoFar;
    private byte[] remainderBytes;

    private static final long DIRECT_UPLOAD_THRESHOLD = 20L<<20;

    private BoxDrain(){}

    public static BoxDrain getInstance(BoxFolder folder, String fileName, long size) throws Exception{
        BoxDrain boxDrain = new BoxDrain();
        boxDrain.isChunkedUpload = size > DIRECT_UPLOAD_THRESHOLD;
        boxDrain.name = fileName;
        boxDrain.size = size;
        boxDrain.folder = folder;
        //Single upload
        if(!boxDrain.isChunkedUpload) {
            boxDrain.folder.canUpload(boxDrain.name, boxDrain.size);
            boxDrain.byteArrayBuilder = new ByteArrayBuilder(Math.toIntExact(size));
        }
        //Chunked upload
        else{
            boxDrain.messageDigest = MessageDigest.getInstance("SHA-1");
            boxDrain.sessionInfo = boxDrain.folder.createUploadSession(fileName, size);
            boxDrain.session = boxDrain.sessionInfo.getResource();
            boxDrain.partSize = boxDrain.sessionInfo.getPartSize();
            boxDrain.parts = new ArrayList<>();
        }
        return boxDrain;
    }

    @Override
    public void drain(Slice slice) throws Exception {
        //Direct upload
        if(!this.isChunkedUpload) {
            this.byteArrayBuilder.write(slice.asBytes());
        }
        //Chunked upload
        else{
            this.remainderBytes = this.remainderBytes != null ?
                    Bytes.concat(this.remainderBytes, slice.asBytes()) : slice.asBytes();
            int offset;
            for(offset = 0; offset + this.partSize <= this.remainderBytes.length; offset += this.partSize){
                //Copy part size bytes in a new byte array
                byte[] partByte = Arrays.copyOfRange(this.remainderBytes, offset, offset + this.partSize);
                BoxFileUploadSessionPart part = this.session.uploadPart(partByte, this.uploadedSoFar, this.partSize,
                        this.size);
                this.parts.add(part);
                this.messageDigest.update(partByte);
            }
            if(this.remainderBytes.length % this.partSize != 0){
                this.remainderBytes = Arrays.copyOfRange(this.remainderBytes, offset, this.remainderBytes.length);
            }
        }
    }

    @Override
    public void finish() throws Exception {
        //Direct upload
        if(!this.isChunkedUpload){
            ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayBuilder.toByteArray());
            this.folder.uploadFile(inputStream, this.name);
        }
        //Chunked upload
        else{
            //Last part may be lesser than the part size
            if (this.remainderBytes.length > 0) {
                BoxFileUploadSessionPart part = this.session.uploadPart(this.remainderBytes, this.uploadedSoFar,
                        this.remainderBytes.length, this.size);
                this.parts.add(part);
                this.uploadedSoFar += this.remainderBytes.length;
                messageDigest.update(this.remainderBytes);
            }

            //Base64 encoding of the hash
            String digest = Base64.getEncoder().encodeToString(messageDigest.digest());
            //TODO: check out attributes that handles other cases like file present
            session.commit(digest, parts, null, null, null);
        }
    }
}