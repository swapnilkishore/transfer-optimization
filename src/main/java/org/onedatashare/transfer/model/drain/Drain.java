package org.onedatashare.transfer.model.drain;

import org.onedatashare.transfer.model.core.Slice;

public interface Drain {
    void drain(Slice slice) throws Exception;
    void finish() throws Exception;
}