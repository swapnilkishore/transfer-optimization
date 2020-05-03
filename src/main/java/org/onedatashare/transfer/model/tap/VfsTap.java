package org.onedatashare.transfer.model.tap;

import org.apache.commons.io.IOUtils;
import org.onedatashare.transfer.model.core.Slice;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;

public final class VfsTap implements Tap {
    private InputStream inputStream;
    private long size;

    private VfsTap(){}

    public static VfsTap initialize(InputStream stream, long size){
        VfsTap vfsTap = new VfsTap();
        vfsTap.inputStream = stream;
        vfsTap.size = size;
        return vfsTap;
    }

    @Override
    public Flux<Slice> openTap(int sliceSize) {
        return Flux.generate(() -> 0L, (state, sink) -> {
            try {
                if (state + sliceSize < this.size) {
                    sink.next(new Slice(IOUtils.toByteArray(inputStream, sliceSize)));
                } else {
                    int remaining = Math.toIntExact(size - state);
                    sink.next(new Slice(IOUtils.toByteArray(inputStream, remaining)));
                    inputStream.close();
                    sink.complete();
                }
            } catch (Exception e){
                sink.error(e);
                e.printStackTrace();
            }
            return state + sliceSize;
        });
    }
}
