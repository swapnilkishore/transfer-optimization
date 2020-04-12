package org.onedatashare.transfer.model.tap;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.DownloadBuilder;
import org.onedatashare.transfer.model.core.Slice;
import org.onedatashare.transfer.model.core.Stat;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class DropboxTap extends Tap {
    DownloadBuilder downloadBuilder;
    long size;

    protected DropboxTap(InputStream inputStream, long size) {
        super(inputStream, size);
    }

    public Flux<Slice> tap(Stat stat, long sliceSize) {
        String downloadPath = "";
//        if(!isFileResource())
//            downloadPath += getPath();
//        downloadBuilder = getSession().getClient().files().downloadBuilder(downloadPath +stat.getName());
        size = stat.getSize();
        return tap(sliceSize);
    }

    public Flux<Slice> tap(long sliceSize) {

        return Flux.generate(
                () -> 0L,
                (state, sink) -> {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    if (state + sliceSize < size) {
                        try {
                            downloadBuilder.range(state, sliceSize).start().download(outputStream);
                        } catch (DbxException | IOException e) {
                            e.printStackTrace();
                        }
                        sink.next(new Slice(outputStream.toByteArray()));
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            downloadBuilder.range(state, size - state).start().download(outputStream);
                        } catch (DbxException | IOException e) {
                            e.printStackTrace();
                        }
                        sink.next(new Slice(outputStream.toByteArray()));
                        sink.complete();
                    }
                    return state + sliceSize;
                });
    }

    @Override
    public Flux<Slice> openTap(int sliceSize) {
        return null;
    }
}
