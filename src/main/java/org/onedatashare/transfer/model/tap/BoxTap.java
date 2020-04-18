package org.onedatashare.transfer.model.tap;

import com.box.sdk.*;
import org.apache.commons.io.IOUtils;
import org.onedatashare.transfer.model.core.Slice;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class BoxTap implements Tap {
    private long size;
    private BoxAPIRequest request;

    private BoxTap(){}

    public static BoxTap initialize(BoxAPIRequest request, long size){
        BoxTap boxTap = new BoxTap();
        boxTap.request = request;
        boxTap.size = size;
        return boxTap;
    }

    /**
     * BoxTap follows a similar model to Google Drive and other transfer modules
     * It uses input and output streams to perform the outgoing transfer
     * @author Javier Falca
     * @param sliceSize
     * @return A flux generated slice
     */
    @Override
    public Flux<Slice> openTap(int sliceSize) {
        int sizeInt = Math.toIntExact(size);
        BoxAPIResponse response = request.send();
        InputStream inputStream = response.getBody();
        return Flux.generate(
                () -> 0,
                (state, sink) -> {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    if (state + sliceSize < sizeInt) {
                        try {
                            IOUtils.copy(inputStream, outputStream);
                        } catch (IOException e) {
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
                            IOUtils.copy(inputStream, outputStream);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        sink.next(new Slice(outputStream.toByteArray()));
                        sink.complete();
                    }
                    return state + sliceSize;
                });
    }
}