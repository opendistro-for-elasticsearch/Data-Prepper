package com.amazon.situp.pipeline.common;

import com.amazon.situp.pipeline.Pipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The {@link Pipeline} process workers which execute pipeline processors and sinks are spun in n different threads
 * which run independently and if any of these threads fail/error or encounter exception, pipeline needs to shutdown.
 * Extending {@link ThreadPoolExecutor} to override {@link ThreadPoolExecutor#afterExecute(Runnable, Throwable)} method
 * to control the behavior of the pipeline when any of the n threads (process worker) encounters an exception or
 * fails/errors.
 *
 */
public class PipelineThreadPoolExecutor extends ThreadPoolExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineThreadPoolExecutor.class);
    private final Pipeline pipeline;

    public PipelineThreadPoolExecutor(
            final int corePoolSize,
            final int maximumPoolSize,
            final long keepAliveTime,
            final TimeUnit unit,
            final BlockingQueue<Runnable> workQueue,
            final ThreadFactory threadFactory,
            final Pipeline pipeline) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        this.pipeline = pipeline;
    }

    public static PipelineThreadPoolExecutor newSingleThreadExecutor(
            final ThreadFactory threadFactory,
            final Pipeline pipeline) {
        return new PipelineThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), threadFactory, pipeline);
    }

    public static PipelineThreadPoolExecutor newFixedThreadPool(
            final int nThreads,
            final ThreadFactory threadFactory,
            final Pipeline pipeline) {
        return new PipelineThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), threadFactory, pipeline);
    }

    /**
     * Overriding the {@link ThreadPoolExecutor#afterExecute(Runnable, Throwable)} to tune the behavior when process
     * worker encounters an exception in one of its worker execution. The below method will be invoked upon completion
     * of execution of the given processWorker Runnable. This method is invoked by the thread that executed the task. If
     * non-null, the Throwable is the uncaught {@code RuntimeException} or {@code Error} that caused execution to
     * terminate abruptly.
     * @param runnable
     * @param throwable
     */
    @Override
    public void afterExecute(final Runnable runnable, final Throwable throwable) {
        super.afterExecute(runnable, throwable);

        // If submit() method is used instead of execute(), the exceptions are wrapped in Future
        // Processor or Sink failures will enter into this loop
        if (throwable == null && runnable instanceof Future<?>) {
            try {
                ((Future<?>) runnable).get();
            } catch (CancellationException | ExecutionException ex) {
                LOG.error("Pipeline [{}] process worker encountered a fatal exception, " +
                        "cannot proceed further", pipeline.getName(), ex);
                pipeline.shutdown();
            } catch (InterruptedException ex) {
                LOG.error("Pipeline [{}] process worker encountered a fatal exception, terminating", pipeline.getName(), ex);
                pipeline.shutdown();
                Thread.currentThread().interrupt();
            }
        }
        // If we ever use the execute instead of submit
        else if (throwable != null) {
            LOG.error("Pipeline {} encountered a fatal exception, terminating", pipeline.getName(), throwable);
            pipeline.shutdown(); //Stop the source and wait for processors to finish draining the buffer
        }
    }

}
