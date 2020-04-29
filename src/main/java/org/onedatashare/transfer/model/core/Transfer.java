package org.onedatashare.transfer.model.core;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.onedatashare.transfer.model.TransferDetails;
import org.onedatashare.transfer.model.drain.Drain;
import org.onedatashare.transfer.model.request.TransferOptions;
import org.onedatashare.transfer.model.tap.Tap;
import org.onedatashare.transfer.model.util.Progress;
import org.onedatashare.transfer.model.util.Throughput;
import org.onedatashare.transfer.model.util.Time;
import org.onedatashare.transfer.model.util.TransferInfo;
import org.onedatashare.transfer.repository.TransferDetailsRepository;
import org.onedatashare.transfer.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


@NoArgsConstructor
@Service
public class Transfer<S extends Resource, D extends Resource> {
    private String id;
    private S source;
    private D destination;
    private List<EntityInfo> filesToTransfer;
    private TransferOptions options;
    private EntityInfo sourceInfo;
    private EntityInfo destinationInfo;
    public static String fName ="";

    private AtomicInteger concurrency = new AtomicInteger(5);

    private ArrayList<Disposable> disposableArrayList = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(Transfer.class);

    /** Periodically updated information about the ongoing transfer. */
    private final TransferInfo info = new TransferInfo();

    // Timer counts 0.0 for files with very small size
    private Time timer;
    private long startTime;
    private Progress progress = new Progress();
    private Throughput throughput = new Throughput();

    @Autowired
    TransferDetailsRepository transferDetailsRepository;

    public Transfer(S source, D destination){
        this.source = source;
        this.destination = destination;
    }

  public Flux start(int sliceSize){
        fName ="This is from transfer.java";
//      transferDetailsRepository.saveAll(Flux.just(new TransferDetails("abc",12l))).subscribe();
        logger.info("Within transfer start");
        return Flux.fromIterable(this.filesToTransfer)
                .doOnSubscribe(s -> {
                    logger.info("Transfer started....");
//                    print(fName);
                    this.startTime = Time.now();
                })
//                .parallel(2)
//                .runOn(Schedulers.elastic())
                .flatMap(file -> {
                    logger.info("Transferring " + file.getPath());
                    Tap tap;
                    try {
                        tap = this.source.getTap(this.sourceInfo, file);
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.error(file + "Unable to read from the tap - " + e.getMessage());
                        return Flux.empty();
                    }
                    Drain drain;
                    try {
                        drain = this.destination.getDrain(this.destinationInfo, file);
                    } catch (Exception e) {
                        logger.error(file + "Unable to create a new file drain - " + e.getMessage());
                        e.printStackTrace();
                        return Flux.empty();
                    }
                    Drain finalDrain = drain;
                    return tap.openTap(sliceSize)
                            .doOnNext(slice -> {

                                logger.info(file.getPath() + " slice received");
                                try {
                                    finalDrain.drain(slice);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            })
                            .doOnComplete(() -> {
                                try {
                                    logger.info("Done transferring " + file.getPath());
                                    finalDrain.finish();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                })
//                .sequential()
                .doOnComplete(() -> {
                    this.startTime = Time.now() - this.startTime;
                    logger.info("Done transferring " + this.id + ". Took "+ startTime/1000 + " secs");

                });
    }
//    public void print(String str){
//        System.out.println(str);
//
//    }
//    public void writeToDB(){
//        transferDetailsRepository.saveAll(Flux.just(new TransferDetails(fName,12l))).subscribe();
//    }


    public TransferInfo addProgress(Slice slice) {
        long size = slice.length();
        progress.add(size);
        throughput.update(size);
        info.update(timer, progress, throughput);
        return info;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<EntityInfo> getFilesToTransfer() {
        return filesToTransfer;
    }

    public void setFilesToTransfer(List<EntityInfo> filesToTransfer) {
        this.filesToTransfer = filesToTransfer;
    }

    public EntityInfo getSourceInfo() {
        return sourceInfo;
    }

    public void setSourceInfo(EntityInfo sourceInfo) {
        this.sourceInfo = sourceInfo;
    }

    public EntityInfo getDestinationInfo() {
        return destinationInfo;
    }

    public void setDestinationInfo(EntityInfo destinationInfo) {
        this.destinationInfo = destinationInfo;
    }
}
