package org.onedatashare.transfer.model.drain;

import org.onedatashare.transfer.model.core.Slice;

public interface Drain {
  Drain start();

  Drain start(String drainPath);    // added for folder transfers

  void drain(Slice slice);

  void finish();

}
