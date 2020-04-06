package org.onedatashare.transfer.model.tap;

import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;
import org.onedatashare.transfer.model.core.Slice;
import org.onedatashare.transfer.model.core.Stat;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;

public class HttpTap implements Tap {

    FileContent fileContent;
    long size;


    @Override
    public Flux<Slice> tap(Stat stat, long sliceSize) {

        try {
            FileObject fileObject = VFS.getManager().resolveFile(stat.getId());
            fileContent = fileObject.getContent();
        } catch (IOException e) {
            e.printStackTrace();
        }
        size = stat.getSize();
        return tap(sliceSize);
    }

    /**
     * This is the tap method that is used to fetch information from the given HTTP file server
     * @param sliceSize : Size of the chunk to be fetched
     * @return Returns a Flux of Slice
     */
    public Flux<Slice> tap(long sliceSize) {
        int sliceSizeInt = Math.toIntExact(sliceSize);
        InputStream inputStream = null;
        try {
            inputStream = fileContent.getInputStream();
        } catch (FileSystemException e) {
            e.printStackTrace();
        }
        InputStream finalInputStream = inputStream;
        return Flux.generate(
                () -> 0,
                (state, sink) -> {
                    if (state + sliceSizeInt < size) {
                        byte[] b = new byte[sliceSizeInt];
                        try {
                            // Fix for buggy PDF files - Else the PDF files are corrupted
                            for(int offset = 0; offset < sliceSizeInt; offset+=1)
                                finalInputStream.read(b, offset, 1);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        sink.next(new Slice(b));
                    } else {
                        int remaining =  Math.toIntExact(size - state);
                        byte[] b = new byte[remaining];
                        try {
                            // Fix for buggy PDF files - Else the PDF files are corrupted
                            for(int offset = 0; offset < remaining; offset+=1)
                                finalInputStream.read(b, offset, 1);
                            sink.next(new Slice(b));
                            finalInputStream.close();
                            sink.complete();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return state + remaining;
                    }
                    return state + sliceSizeInt;
                });
    }
}
