package org.onedatashare.transfer.model.tap;


import org.onedatashare.transfer.model.core.Slice;
import reactor.core.publisher.Flux;

public interface Tap {
  Flux<Slice> openTap(int sliceSize);
}
