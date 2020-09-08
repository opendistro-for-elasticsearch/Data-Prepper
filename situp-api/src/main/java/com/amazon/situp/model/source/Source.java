package com.amazon.situp.model.source;

import com.amazon.situp.model.buffer.Buffer;
import com.amazon.situp.model.record.Record;

/**
 * SITUP source interface. Source acts as receiver of the events that flow
 * through the transformation pipeline.
 */
public interface Source<T extends Record<?>> {

    /**
     * Notifies the source to start writing the records into the buffer
     *
     * @param buffer Buffer to which the records will be queued or written to.
     */
    void start(Buffer<T> buffer);

    /**
     * Notifies the source to stop consuming/writing records into Buffer.
     */
    void stop();
}
