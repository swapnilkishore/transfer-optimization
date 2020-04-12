package org.onedatashare.transfer.model.tap;

import com.google.api.client.http.GenericUrl;
import com.google.api.services.drive.Drive;
import org.apache.commons.io.IOUtils;
import org.onedatashare.transfer.model.core.Slice;
import org.onedatashare.transfer.model.core.Stat;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class GoogleDriveTap extends Tap {
    long size;
    Drive drive;
    com.google.api.client.http.HttpRequest httpRequestGet;

    protected GoogleDriveTap(InputStream inputStream, long size) {
        super(inputStream, size);
    }

    public Flux<Slice> tap(Stat stat, long sliceSize) {

        String downloadUrl = "https://www.googleapis.com/drive/v3/files/"+stat.getId()+"?alt=media";
        try {
            httpRequestGet = drive.getRequestFactory().buildGetRequest(new GenericUrl(downloadUrl));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                            httpRequestGet.getHeaders().setRange("bytes=" + state + "-" + (state + sliceSize - 1));
                            com.google.api.client.http.HttpResponse response = httpRequestGet.execute();
                            InputStream is = response.getContent();
                            IOUtils.copy(is, outputStream);
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
                            httpRequestGet.getHeaders().setRange("bytes=" + state + "-" + (size - 1));
                            com.google.api.client.http.HttpResponse response = httpRequestGet.execute();
                            InputStream is = response.getContent();
                            IOUtils.copy(is, outputStream);
                        } catch (IOException e) {
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
