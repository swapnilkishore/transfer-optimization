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


@NoArgsConstructor
@Data
public class Transfer<S extends Resource, D extends Resource> {
    public S source;
    public D destination;
    public List<IdMap> filesToTransfer;
    public TransferOptions options;

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

  public void start(int sliceSize){
        Flux.fromIterable(filesToTransfer)
                .flatMap(file -> {
                    logger.info("Transferring file " + file.getUri());
                    Tap tap = source.getTap(file);
                    Drain drain = destination.getDrain(file);
                    tap.openTap(sliceSize)
                            .doOnNext(slice -> {
                                try {
                                    drain.drain(slice);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            })
                            .map(this::addProgress)
                            .doOnComplete(() -> {
                                try {
                                    drain.finish();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            })
                            .subscribeOn(Schedulers.elastic())
                            .subscribe();
                    return Flux.empty();
                });
    }

    public Flux<TransferInfo> blockingStart(int sliceSize){
        for(IdMap file : filesToTransfer){
            logger.info("Transferring file " + file.getUri());
            Tap tap = source.getTap(file);
            Drain drain = destination.getDrain(file);
            tap.openTap(sliceSize)
                    .doOnNext(slice -> {
                        try {
                            drain.drain(slice);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    })
                    .map(this::addProgress)
                    .doOnComplete(() -> {
                        try {
                            drain.finish();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    })
                    .subscribeOn(Schedulers.elastic())
                    .blockLast();
        }
        return Flux.just(new TransferInfo());
    }


    public TransferInfo addProgress(Slice slice) {
        long size = slice.length();
        progress.add(size);
        throughput.update(size);
        info.update(timer, progress, throughput);
        return info;
    }
}
