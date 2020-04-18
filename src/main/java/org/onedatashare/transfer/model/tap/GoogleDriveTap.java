package org.onedatashare.transfer.model.tap;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import org.apache.commons.io.IOUtils;
import org.onedatashare.transfer.model.core.Slice;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public final class GoogleDriveTap implements Tap {
    private long size;
    private HttpRequest request;

    private GoogleDriveTap(){}

    public static GoogleDriveTap getInstance(HttpRequest request, long size){
        GoogleDriveTap driveTap = new GoogleDriveTap();
        driveTap.request = request;
        driveTap.size = size;
        return driveTap;
    }

    @Override
    public Flux<Slice> openTap(int sliceSize) {
        return Flux.generate(() -> 0L, (state, sink) -> {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                if (state + sliceSize < size) {
                    request.getHeaders().setRange("bytes=" + state + "-" + (state + sliceSize - 1));
                    HttpResponse response = request.execute();
                    InputStream inputStream = response.getContent();
                    sink.next(new Slice(IOUtils.toByteArray(inputStream)));
                    outputStream.close();
                } else {
                    request.getHeaders().setRange("bytes=" + state + "-" + (size - 1));
                    HttpResponse response = request.execute();
                    InputStream inputStream = response.getContent();
                    sink.next(new Slice(IOUtils.toByteArray(inputStream)));
                    sink.complete();
                }
            } catch (Exception e){
                e.printStackTrace();
                sink.error(e);
            }
            return state + sliceSize;
        });
    }
}