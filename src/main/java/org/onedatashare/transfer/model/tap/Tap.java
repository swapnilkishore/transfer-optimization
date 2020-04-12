package org.onedatashare.transfer.model.tap;


import org.onedatashare.transfer.model.core.Slice;
import reactor.core.publisher.Flux;

import java.io.InputStream;

public abstract class Tap {
  protected InputStream inputStream;
  protected long size;

  protected Tap(InputStream inputStream, long size){
    this.inputStream = inputStream;
    this.size = size;
  }

  public abstract Flux<Slice> openTap(int sliceSize);
}
