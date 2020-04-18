package org.onedatashare.transfer.model.tap;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.DownloadBuilder;
import org.onedatashare.transfer.model.core.Slice;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class DropboxTap implements Tap {
    DownloadBuilder downloadBuilder;
    long size;

    public static DropboxTap initialize(String url , DbxUserFilesRequests requests, long size){
        DropboxTap dropboxTap = new DropboxTap();
        dropboxTap.downloadBuilder = requests.downloadBuilder(url);
        dropboxTap.size = size;
        return dropboxTap;
    }

    @Override
    public Flux<Slice> openTap(int sliceSize) {
        return Flux.generate(() -> 0L, (state, sink) -> {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try{
                if (state + sliceSize < size) {
                    downloadBuilder.range(state, sliceSize).start().download(outputStream);
                    sink.next(new Slice(outputStream.toByteArray()));
                    outputStream.close();
                } else {
                    downloadBuilder.range(state, size - state).start().download(outputStream);
                    sink.next(new Slice(outputStream.toByteArray()));
                    sink.complete();
                }
            } catch (IOException | DbxException e) {
                sink.error(e);
            }
            return state + sliceSize;
        });

    }
}
