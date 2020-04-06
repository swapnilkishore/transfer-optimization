package org.onedatashare.transfer.model.tap;

import org.onedatashare.transfer.model.core.Slice;
import org.onedatashare.transfer.model.core.Stat;
import reactor.core.publisher.Flux;

public interface Tap {
  Flux<Slice> tap(long sliceSize);
  Flux<Slice> tap(Stat stat, long sliceSize);
}
