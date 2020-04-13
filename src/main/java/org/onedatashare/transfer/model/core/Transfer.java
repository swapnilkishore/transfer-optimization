package org.onedatashare.transfer.model.core;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.onedatashare.transfer.model.drain.Drain;
import org.onedatashare.transfer.model.request.TransferOptions;
import org.onedatashare.transfer.model.tap.Tap;
import org.onedatashare.transfer.model.util.Progress;
import org.onedatashare.transfer.model.util.Throughput;
import org.onedatashare.transfer.model.util.Time;
import org.onedatashare.transfer.model.util.TransferInfo;
import org.onedatashare.transfer.module.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


@NoArgsConstructor
@Data
public class Transfer<S extends Resource, D extends Resource> {
    public S source;
    public D destination;
    public List<IdMap> filesToTransfer;
    public TransferOptions options;
    public String sourceBaseUri;
    public String destinationBaseUri;

    public AtomicInteger concurrency = new AtomicInteger(5);

    private ArrayList<Disposable> disposableArrayList = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(Transfer.class);

    /** Periodically updated information about the ongoing transfer. */
    public final TransferInfo info = new TransferInfo();

    // Timer counts 0.0 for files with very small size
    protected Time timer;
    protected Progress progress = new Progress();
    protected Throughput throughput = new Throughput();

    public Transfer(S source, D destination){
        this.source = source;
        this.destination = destination;
    }

  public Flux start(int sliceSize){
        logger.info("Within transfer start");
        return Flux.fromIterable(filesToTransfer)
                .doOnSubscribe(s -> logger.info("Transfer started...."))
                .flatMap(file -> {
                    logger.info("Transferring " + file.getUri());
                    Tap tap;
                    try {
                        tap = source.getTap(file, sourceBaseUri);
                    } catch (Exception e) {
                        logger.error("Unable to read from the tap - " + e.getMessage());
                        return Flux.empty();
                    }
                    Drain drain;
                    try {
                        drain = destination.getDrain(file, destinationBaseUri);
                    } catch (Exception e) {
                        logger.error("Unable to create a new file drain - " + e.getMessage());
                        return Flux.empty();
                    }
                    Drain finalDrain = drain;
                    return tap.openTap(sliceSize)
                            .doOnNext(slice -> {
                                logger.info("slice recieveds");
                                try {
                                    finalDrain.drain(slice);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            })
                            .doOnComplete(() -> {
                                try {
                                    finalDrain.finish();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            })
                            .subscribeOn(Schedulers.elastic());
                }).doOnComplete(() -> logger.info("Done transferring"));
    }

    public TransferInfo addProgress(Slice slice) {
        long size = slice.length();
        progress.add(size);
        throughput.update(size);
        info.update(timer, progress, throughput);
        return info;
    }
}
