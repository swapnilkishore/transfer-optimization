package org.onedatashare.transfer.model.tap;

import com.box.sdk.*;
import com.box.sdk.http.HttpMethod;
import org.apache.commons.io.IOUtils;
import org.onedatashare.transfer.model.core.Slice;
import org.onedatashare.transfer.model.core.Stat;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class BoxTap implements Tap {
    long size;
    BoxAPIConnection api = null;//getSession().client;
    BoxAPIRequest req;

    public Flux<Slice> tap(Stat stat, long sliceSize) {
        BoxFile file = new BoxFile(api, stat.getId());
        try {
            URL downloadUrl = file.getDownloadURL();
            req = new BoxAPIRequest(api, downloadUrl, HttpMethod.GET);
        }catch(BoxAPIResponseException be){
            if(be.getResponseCode() == 403){
                return Flux.error(be);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        size = stat.getSize();
        return tap(sliceSize);

    }

    /**
     * BoxTap follows a similar model to Google Drive and other transfer modules
     * It uses input and output streams to perform the outgoing transfer
     * @author Javier Falca
     * @param sliceSize
     * @return A flux generated slice
     */
    public Flux<Slice> tap(long sliceSize) {

        int sliceSizeInt = Math.toIntExact(sliceSize);
        int sizeInt = Math.toIntExact(size);
        BoxAPIResponse resp = req.send();
        InputStream inputStream = resp.getBody();
        return Flux.generate(
                () -> 0,
                (state, sink) -> {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    if (state + sliceSizeInt < sizeInt) {
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
                    return state + sliceSizeInt;
                });

    }

    @Override
    public Flux<Slice> openTap(int sliceSize) {
        return null;
    }
}
