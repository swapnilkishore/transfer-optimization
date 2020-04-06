package org.onedatashare.transfer.model.tap;

import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileSystemException;
import org.onedatashare.transfer.model.core.Slice;
import org.onedatashare.transfer.model.core.Stat;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;

public class VfsTap implements Tap {
    FileContent fileContent;
    long size;

    @Override
    public Flux<Slice> tap(Stat stat, long sliceSize) {
        String downloadPath = "";//getPath();
//        if (!isFileResource())
//            downloadPath += stat.getName();
//        try {
//            fileContent = getSession().fileSystemManager.resolveFile(downloadPath, getSession().fileSystemOptions).getContent();
//        } catch (FileSystemException e) {
//            e.printStackTrace();
//        }
        size = stat.getSize();
        return tap(sliceSize);
    }

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
                () -> 0L,
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
                        int remaining = Math.toIntExact(size - state);
                        byte[] b = new byte[remaining];
                        try {
                            // Fix for buggy PDF files - Else the PDF files are corrupted
                            for(int offset = 0; offset < remaining; offset+=1)
                                finalInputStream.read(b, offset, 1);
                            finalInputStream.close();
                            sink.next(new Slice(b));
                            sink.complete();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    return state + sliceSizeInt;
                });
    }
}
