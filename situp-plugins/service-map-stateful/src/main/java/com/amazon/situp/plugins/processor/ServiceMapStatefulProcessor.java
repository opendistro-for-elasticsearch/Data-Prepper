package com.amazon.situp.plugins.processor;

import com.amazon.situp.model.PluginType;
import com.amazon.situp.model.annotations.SitupPlugin;
import com.amazon.situp.model.configuration.PluginSetting;
import com.amazon.situp.model.processor.Processor;
import com.amazon.situp.model.record.Record;
import com.amazon.situp.plugins.processor.state.MapDbProcessorState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.SignedBytes;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SitupPlugin(name = "service_map_stateful", type = PluginType.PROCESSOR)
public class ServiceMapStatefulProcessor implements Processor<Record<ExportTraceServiceRequest>, Record<String>> {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceMapStatefulProcessor.class);
    private static final String EMPTY_SUFFIX = "-empty";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Collection<Record<String>> EMPTY_COLLECTION = Collections.emptySet();
    private static final Integer TO_MILLIS = 1_000;
    private static final AtomicInteger processorsCreated = new AtomicInteger(0);
    private static long previousTimestamp;
    private static long windowDurationMillis;
    private static CountDownLatch edgeEvaluationLatch;
    private static CountDownLatch windowRotationLatch = new CountDownLatch(1);
    private volatile static MapDbProcessorState<ServiceMapStateData> previousWindow;
    private volatile static MapDbProcessorState<ServiceMapStateData> currentWindow;
    private volatile static MapDbProcessorState<String> previousTraceGroupWindow;
    private volatile static MapDbProcessorState<String> currentTraceGroupWindow;
    //TODO: Consider keeping this state in lmdb
    private volatile static  HashSet<ServiceMapRelationship> relationshipState = new HashSet<>();
    private static File dbPath;
    private static Clock clock;

    private final int thisProcessorId;

    public ServiceMapStatefulProcessor(final PluginSetting pluginSetting) {
     this(pluginSetting.getIntegerOrDefault(ServiceMapProcessorConfig.WINDOW_DURATION, ServiceMapProcessorConfig.DEFAULT_WINDOW_DURATION)*TO_MILLIS,
             new File(ServiceMapProcessorConfig.DEFAULT_LMDB_PATH),
             Clock.systemUTC());
    }

    public ServiceMapStatefulProcessor(final long windowDurationMillis,
                                       final File databasePath,
                                       final Clock clock) {
        ServiceMapStatefulProcessor.clock = clock;
        this.thisProcessorId = processorsCreated.getAndIncrement();
        if(isMasterInstance()) {
            previousTimestamp = ServiceMapStatefulProcessor.clock.millis();
            ServiceMapStatefulProcessor.windowDurationMillis = windowDurationMillis;
            ServiceMapStatefulProcessor.dbPath = createPath(databasePath);
            currentWindow = new MapDbProcessorState<>(dbPath, getNewDbName());
            previousWindow = new MapDbProcessorState<>(dbPath, getNewDbName() + EMPTY_SUFFIX);
            currentTraceGroupWindow = new MapDbProcessorState<>(dbPath, getNewTraceDbName());
            previousTraceGroupWindow = new MapDbProcessorState<>(dbPath, getNewTraceDbName() + EMPTY_SUFFIX);
        }
    }

    /**
     * This function creates the directory if it doesn't exists and returns the File.
     * @param path
     * @return path
     * @throws RuntimeException if the directory can not be created.
     */
    private static File createPath(File path) {
        if(!path.exists()){
            if(!path.mkdirs()){
                throw new RuntimeException(String.format("Unable to create the directory at the provided path: %s", path.getName()));
            }
        }
        return path;
    }

    /**
     * Adds the data for spans from the ResourceSpans object to the current window
     * @param records Input records that will be modified/processed
     * @return If the window is reached, returns a list of ServiceMapRelationship objects representing the edges to be
     * added to the service map index. Otherwise, returns an empty set.
     */
    @Override
    public Collection<Record<String>> execute(Collection<Record<ExportTraceServiceRequest>> records) {
        final Collection<Record<String>> relationships = windowDurationHasPassed() ? evaluateEdges() : EMPTY_COLLECTION;
        final Map<byte[], ServiceMapStateData> batchStateData = new TreeMap<>(SignedBytes.lexicographicalComparator());
        records.forEach( i -> i.getData().getResourceSpansList().forEach(resourceSpans -> {
            OTelHelper.getServiceName(resourceSpans.getResource()).ifPresent(serviceName -> resourceSpans.getInstrumentationLibrarySpansList().forEach(
                    instrumentationLibrarySpans -> {
                        instrumentationLibrarySpans.getSpansList().forEach(
                                span -> {
                                    if (OTelHelper.checkValidSpan(span)) {
                                        try {
                                            batchStateData.put(
                                                    span.getSpanId().toByteArray(),
                                                    new ServiceMapStateData(
                                                            serviceName,
                                                            span.getParentSpanId().isEmpty() ? null : span.getParentSpanId().toByteArray(),
                                                            span.getTraceId().toByteArray(),
                                                            span.getKind().name(),
                                                            span.getName()));
                                        } catch (RuntimeException e) {
                                            LOG.error("Caught exception trying to put service map state data into batch", e);
                                        }
                                        if (span.getParentSpanId().isEmpty()) {
                                            try {
                                                currentTraceGroupWindow.put(span.getTraceId().toByteArray(), span.getName());
                                            } catch (RuntimeException e) {
                                                LOG.error("Caught exception trying to put trace group name", e);
                                            }
                                        }
                                    } else {
                                        LOG.warn("Invalid span received");
                                    }
                                });
                    }
            ));
        }));
        try {
            currentWindow.putAll(batchStateData);
        } catch (RuntimeException e) {
            LOG.error("Caught exception trying to put batch state data", e);
        }
        return relationships;
    }

    /**
     * This function parses the current and previous windows to find the edges, and rotates the window state objects.
     * @return Set of Record<String> containing json representation of ServiceMapRelationships found
     */
    private Collection<Record<String>> evaluateEdges() {
        try {
            final Stream<ServiceMapRelationship> previousStream = previousWindow.iterate(realtionshipIterationFunction, processorsCreated.get(), thisProcessorId).stream().flatMap(serviceMapEdgeStream -> serviceMapEdgeStream);
            final Stream<ServiceMapRelationship> currentStream = currentWindow.iterate(realtionshipIterationFunction, processorsCreated.get(), thisProcessorId).stream().flatMap(serviceMapEdgeStream -> serviceMapEdgeStream);

            final Collection<Record<String>> serviceDependencyRecords =
                    Stream.concat(previousStream, currentStream).filter(Objects::nonNull)
                            .filter(serviceMapRelationship -> !relationshipState.contains(serviceMapRelationship))
                            .map(serviceMapRelationship -> {
                                try {
                                    relationshipState.add(serviceMapRelationship);
                                    return new Record<>(OBJECT_MAPPER.writeValueAsString(serviceMapRelationship));
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .collect(Collectors.toSet());

            if(edgeEvaluationLatch == null) {
                initEdgeEvaluationLatch();
            }
            doneEvaluatingEdges();
            waitForEvaluationFinish();

            if(isMasterInstance()) {
                rotateWindows();
                resetWorkState();
            } else {
                waitForRotationFinish();
            }

            return serviceDependencyRecords;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static synchronized void initEdgeEvaluationLatch() {
        if(edgeEvaluationLatch==null){
            edgeEvaluationLatch = new CountDownLatch(processorsCreated.get());
        }
    }

    /**
     * This function is used to iterate over the current window and find parent/child relationships in the current and
     * previous windows.
     */
    private final BiFunction<byte[], ServiceMapStateData, Stream<ServiceMapRelationship>> realtionshipIterationFunction = new BiFunction<byte[], ServiceMapStateData, Stream<ServiceMapRelationship>>() {
        @Override
        public Stream<ServiceMapRelationship> apply(byte[] s, ServiceMapStateData serviceMapStateData) {
            return lookupParentSpan(serviceMapStateData, true);
        }
    };

    private Stream<ServiceMapRelationship> lookupParentSpan(final ServiceMapStateData serviceMapStateData, final boolean checkPrev) {
        if (serviceMapStateData.parentSpanId != null) {
            final ServiceMapStateData parentStateData = getParentStateData(serviceMapStateData.parentSpanId, checkPrev);
            final String traceGroupName = getTraceGroupName(serviceMapStateData.traceId);
            if (traceGroupName != null && parentStateData != null && !parentStateData.serviceName.equals(serviceMapStateData.serviceName)) {
                return Stream.of(
                        ServiceMapRelationship.newDestinationRelationship(parentStateData.serviceName, parentStateData.spanKind, serviceMapStateData.serviceName, serviceMapStateData.name, traceGroupName),
                        //This extra edge is added for compatibility of the index for both stateless and stateful processors
                        ServiceMapRelationship.newTargetRelationship(serviceMapStateData.serviceName, serviceMapStateData.spanKind, serviceMapStateData.serviceName, serviceMapStateData.name, traceGroupName)
                );
            }
        }
        return Stream.empty();
    }

    /**
     * Checks both current and previous windows for the given parent span id
     *
     * @param spanId
     * @return ServiceMapStateData for the parent span, if exists. Otherwise null
     */
    private ServiceMapStateData getParentStateData(final byte[] spanId, final boolean checkPrev) {
        try {
            final ServiceMapStateData serviceMapStateData = currentWindow.get(spanId);
            return serviceMapStateData != null ? serviceMapStateData : checkPrev ? previousWindow.get(spanId) : null;
        } catch (RuntimeException e) {
            LOG.error("Caught exception trying to get parent state data", e);
            return null;
        }
    }

    /**
     *  Checks both current and previous trace group windows for the trace id
     * @param traceId
     * @return Trace group name for the given trace if it exists. Otherwise null.
     */
    private String getTraceGroupName(final byte[] traceId) {
        try {
            final String traceGroupName = currentTraceGroupWindow.get(traceId);
            return traceGroupName != null ? traceGroupName : previousTraceGroupWindow.get(traceId);
        } catch (RuntimeException e) {
            LOG.error("Caught exception trying to get trace group name", e);
            return null;
        }
    }

    //TODO: Change to an override when a shutdown method is added to the processor interface
    public void shutdown() {
        previousWindow.delete();
        currentWindow.delete();
        previousTraceGroupWindow.delete();
        currentTraceGroupWindow.delete();
    }

    /**
     * Indicate/notify that this instance has finished evaluating edges
     */
    private void doneEvaluatingEdges() {
        edgeEvaluationLatch.countDown();
    }

    /**
     * Wait on all instances to finish evaluating edges
     * @throws InterruptedException
     */
    private void waitForEvaluationFinish() throws InterruptedException {
        edgeEvaluationLatch.await();
    }

    /**
     * Indicate that window rotation is complete
     */
    private void doneRotatingWindows() {
        windowRotationLatch.countDown();
    }

    /**
     * Wait on window rotation to complete
     * @throws InterruptedException
     */
    private void waitForRotationFinish() throws InterruptedException {
        windowRotationLatch.await();
    }

    /**
     * Reset state that indicates whether edge evaluation and window rotation is complete
     */
    private void resetWorkState() {
        windowRotationLatch = new CountDownLatch(1);
        edgeEvaluationLatch = new CountDownLatch(processorsCreated.get());
    }

    /**
     * Rotate windows for processor state
     */
    private void rotateWindows() {
        LOG.debug("Rotating windows at " + clock.instant().toString());
        previousWindow.delete();
        previousTraceGroupWindow.delete();
        previousWindow = currentWindow;
        currentWindow = new MapDbProcessorState<>(dbPath, getNewDbName());
        previousTraceGroupWindow = currentTraceGroupWindow;
        currentTraceGroupWindow = new MapDbProcessorState<>(dbPath, getNewTraceDbName());
        previousTimestamp = clock.millis();
        doneRotatingWindows();
    }

    /**
     * @return Next database name
     */
    private String getNewDbName() {
        return "db-" + clock.millis();
    }

    /**
     * @return Next database name
     */
    private String getNewTraceDbName() {
        return "trace-db-" + clock.millis();
    }

    /**
     * @return Boolean indicating whether the window duration has lapsed
     */
    private boolean windowDurationHasPassed() {
        if ((clock.millis() - previousTimestamp)  >= windowDurationMillis) {
            return true;
        }
        return false;
    }

    /**
     * Master instance is needed to do things like window rotation that should only be done once
     * @return Boolean indicating whether this object is the master ServiceMapStatefulProcessor instance
     */
    private boolean isMasterInstance() {
        return thisProcessorId == 0;
    }

    private static class ServiceMapStateData implements Serializable {
        public String serviceName;
        public byte[] parentSpanId;
        public byte[] traceId;
        public String spanKind;
        public String name;

        public ServiceMapStateData() {
        }

        public ServiceMapStateData(final String serviceName, final byte[] parentSpanId,
                                   final byte[] traceId,
                                   final String spanKind,
                                   final String name) {
            this.serviceName = serviceName;
            this.parentSpanId = parentSpanId;
            this.traceId = traceId;
            this.spanKind = spanKind;
            this.name = name;
        }
    }
}