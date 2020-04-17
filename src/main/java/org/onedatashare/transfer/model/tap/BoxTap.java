package org.onedatashare.transfer.model.tap;

import com.box.sdk.*;
import com.box.sdk.http.HttpMethod;
import org.apache.commons.io.IOUtils;
import org.onedatashare.transfer.model.core.Slice;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class BoxTap implements Tap {
    long size;
    BoxAPIRequest req;

    private BoxTap(){}

    public static BoxTap initialize(BoxAPIConnection connection, String id, long size){
        BoxTap boxTap = new BoxTap();
        BoxFile file = new BoxFile(connection, id);
        URL downloadUrl = file.getDownloadURL();
        boxTap.req = new BoxAPIRequest(connection, downloadUrl, HttpMethod.GET);
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
        BoxAPIResponse resp = req.send();
        InputStream inputStream = resp.getBody();
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