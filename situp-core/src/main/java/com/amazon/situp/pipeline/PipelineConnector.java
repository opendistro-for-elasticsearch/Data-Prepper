package com.amazon.situp.pipeline;

import com.amazon.situp.model.buffer.Buffer;
import com.amazon.situp.model.record.Record;
import com.amazon.situp.model.sink.Sink;
import com.amazon.situp.model.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PipelineConnector is a special type of Plugin which connects two pipelines acting both as Sink and Source.
 *
 * @param <T>
 */
public final class PipelineConnector<T extends Record<?>> implements Source<T>, Sink<T> {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineConnector.class);
    private static final int DEFAULT_WRITE_TIMEOUT = Integer.MAX_VALUE;
    private Buffer<T> buffer;
    private AtomicBoolean isStopRequested;

    public PipelineConnector() {
        isStopRequested = new AtomicBoolean(false);
    }

    @Override
    public void start(final Buffer<T> buffer) {
        this.buffer = buffer;
    }

    @Override
    public void stop() {
        //this.buffer = null;
        isStopRequested.set(true);
    }

    @Override
    public boolean output(final Collection<T> records) {
        if (buffer != null && !isStopRequested.get()) {
            for (T record : records) {
                try {
                    buffer.write(record, DEFAULT_WRITE_TIMEOUT); //TODO update to use from config
                } catch (TimeoutException ex) {
                    LOG.error("Timed out writing to pipeline source", ex);
                    return false;
                }
            }
            return true;
        } else {
            LOG.error("Pipeline source is currently not initialized or has been halted");
            return false;
        }
    }
}
