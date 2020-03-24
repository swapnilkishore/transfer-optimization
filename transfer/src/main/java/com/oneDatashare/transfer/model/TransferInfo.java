package com.oneDatashare.transfer.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;

import java.beans.Transient;

import static com.oneDatashare.transfer.model.core.ODSConstants.TRANSFER_SLICE_SIZE;

/**
 * This is used to track the progress of a transfer in real time.
 */
@NoArgsConstructor
@Data
public class   TransferInfo {
    /** Units complete. */
    public long done;
    /** Total units. */
    public long total;
    /** Average throughput. */
    public double avg;
    /** Instantaneous throughput. */
    public double inst;

    @Transient
    private long lastTime = Time.now();

    /*Reason for failure */
    public String reason;

    /** Update based on the given information. */
    public void update(Time time, Progress p, Throughput tp) {
        done = p.done();
        avg = p.rate(time).value();
        long currTime = time.elapsed();
        inst = TRANSFER_SLICE_SIZE / (currTime - lastTime);
        lastTime = currTime;
    }

    public TransferInfo(long total) {
        this.total = total;
    }

    public void setTotal(long maxValue) {
    }
}