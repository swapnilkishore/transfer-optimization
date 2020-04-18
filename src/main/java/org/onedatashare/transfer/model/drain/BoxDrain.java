package org.onedatashare.transfer.model.drain;

import com.box.sdk.*;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import org.onedatashare.transfer.model.core.Slice;

import java.io.ByteArrayInputStream;

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
    private ByteArrayBuilder byteArrayBuilder;
    private boolean isSmall;

    private static final long SINGLE_UPLOAD_THRESHOLD = 20L<<20;
    private static final long CHUNKED_UPLOAD_SIZE = 8L<<20;

    private BoxDrain(){}

    public static BoxDrain getInstance(BoxFolder boxFolder, String fileName, long size) throws Exception{
        BoxDrain boxDrain = new BoxDrain();
        boxDrain.isSmall = size < SINGLE_UPLOAD_THRESHOLD;
        boxDrain.name = fileName;
        boxDrain.size = size;
        boxDrain.folder = boxFolder;
        boxDrain.folder.canUpload(boxDrain.name, boxDrain.size);
        boxDrain.byteArrayBuilder = new ByteArrayBuilder(Math.toIntExact(size));
        return boxDrain;
    }

    @Override
    public void drain(Slice slice) throws Exception {
        byteArrayBuilder.write(slice.asBytes());
    }

    @Override
    public void finish() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayBuilder.toByteArray());
        this.folder.uploadFile(inputStream, this.name);
    }
}