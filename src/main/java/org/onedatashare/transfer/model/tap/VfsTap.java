package org.onedatashare.transfer.model.tap;

import org.onedatashare.transfer.model.core.Slice;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;

public class VfsTap extends Tap {

    public VfsTap(InputStream inputStream, long size){
        super(inputStream, size);
    }

    @Override
    public Flux<Slice> openTap(int sliceSize) {
        return Flux.generate(() -> 0L, (state, sink) -> {
            if (state + sliceSize < size) {
                byte[] b = new byte[sliceSize];
                try {
                    // Fix for buggy PDF files - Else the PDF files are corrupted
                    for(int offset = 0; offset < sliceSize; offset+=1)
                        inputStream.read(b, offset, 1);
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
                        inputStream.read(b, offset, 1);
                    inputStream.close();
                    sink.next(new Slice(b));
                    sink.complete();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return state + sliceSize;
        });
    }
}
