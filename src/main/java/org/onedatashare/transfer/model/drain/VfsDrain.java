package org.onedatashare.transfer.model.drain;

import org.onedatashare.transfer.model.core.Slice;

import java.io.OutputStream;

public class VfsDrain implements Drain {
    OutputStream outputStream;

    private VfsDrain(){}

    public static VfsDrain initialize(OutputStream stream){
        VfsDrain vfsDrain = new VfsDrain();
        vfsDrain.outputStream = stream;
        return vfsDrain;
    }

    @Override
    public void drain(Slice slice) throws Exception{
            this.outputStream.write(slice.asBytes());
            this.outputStream.flush();
    }

    @Override
    public void finish() throws Exception{
        this.outputStream.flush();
        this.outputStream.close();
    }
}