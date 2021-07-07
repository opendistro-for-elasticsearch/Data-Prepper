package com.amazon.dataprepper.benchmarks.prepper;

import com.amazon.dataprepper.model.configuration.PluginSetting;
import com.amazon.dataprepper.model.record.Record;
import com.amazon.dataprepper.plugins.prepper.oteltrace.OTelTraceRawPrepper;
import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.InstrumentationLibrarySpans;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Span;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class OtelTraceRawPrepperBenchmarks {

    private static final int OPERATIONS_PER_INVOCATION = 10;

    @State(Scope.Benchmark)
    public static class OtelTraceRawPrepperState {
        private OTelTraceRawPrepper otelTraceRawPrepper;

        @Param(value = "4")
        private int concurrencyScale;

        @Setup(Level.Trial)
        public void setupOtelTraceRawPrepper() {
            PluginSetting pluginSetting = new PluginSetting("otel_trace_raw_prepper", new HashMap<>()) {{
                setPipelineName("test-pipeline");
                setProcessWorkers(concurrencyScale);
            }};
            otelTraceRawPrepper = new OTelTraceRawPrepper(pluginSetting);
        }

        @TearDown(Level.Trial)
        public void shutdownOtelTraceRawPrepper() {
            otelTraceRawPrepper.shutdown();
        }
    }

    @State(Scope.Thread)
    public static class TestDataState {
        private List<byte[]> traceIds = new ArrayList<>();
        private Map<byte[], byte[]> traceIdToRootSpanId = new HashMap<>();
        private Map<byte[], List<byte[]>> traceIdToSpanIds = new HashMap<>();
        private static final Random RANDOM = new Random();
        private static final List<String> serviceNames = Arrays.asList("FRONTEND", "BACKEND", "PAYMENT", "CHECKOUT", "DATABASE");
        private static final List<String> traceGroups = Arrays.asList("tg1", "tg2", "tg3", "tg4", "tg5", "tg6", "tg7", "tg8", "tg9");
        private List<Record<ExportTraceServiceRequest>> bulk;

        @Param(value = "10")
        protected int batchSize;

        /**
         * Gets a new trace id. 10% of the time it will generate a new id, and otherwise will pick a random
         * trace id that has already been generated
         */
        private byte[] getTraceId() {
            if(RANDOM.nextInt(100) < 10 || traceIds.isEmpty()) {
                final byte[] traceId = getRandomBytes(16);
                traceIds.add(traceId);
                return traceId;
            } else {
                return traceIds.get(RANDOM.nextInt(traceIds.size()));
            }
        }

        /**
         * Gets a span id and adds to the list of existing span ids
         */
        private byte[] getSpanId(final byte[] traceId) {
            final byte[] spanId = getRandomBytes(8);
            if (!traceIdToRootSpanId.containsKey(traceId)) {
                traceIdToRootSpanId.put(traceId, spanId);
            }
            traceIdToSpanIds.compute(traceId, (trId, spanIds) -> {
                if (spanIds == null) {
                    spanIds = new ArrayList<>();
                }
                spanIds.add(spanId);
                return spanIds;
            });
            return spanId;
        }

        private byte[] getParentId(final byte[] traceId, final byte[] spanId) {
            if(traceIdToRootSpanId.get(traceId) == spanId) {
                return null;
            } else {
                final List<byte[]> spanIds = traceIdToSpanIds.get(traceId);
                return spanIds.get(RANDOM.nextInt(spanIds.size()));
            }
        }

        @Setup(Level.Invocation)
        public void resetTraceSpanIdCaches() {
            traceIdToRootSpanId = new HashMap<>();
            traceIdToSpanIds = new HashMap<>();
            traceIds = new ArrayList<>();
        }

        @Setup(Level.Invocation)
        public void generateBatch() {
            bulk = new ArrayList<>();
            for(int j=0; j<batchSize*OPERATIONS_PER_INVOCATION; j++) {
                final byte[] traceId = getTraceId();
                final byte[] spanId = getSpanId(traceId);
                final byte[] parentId = getParentId(traceId, spanId);
                bulk.add(new Record<>(getExportTraceServiceRequest(
                        getResourceSpans(
                                serviceNames.get(RANDOM.nextInt(serviceNames.size())),
                                traceGroups.get(RANDOM.nextInt(traceGroups.size())),
                                spanId,
                                parentId,
                                traceId,
                                Span.SpanKind.SPAN_KIND_CLIENT
                        ))));
            }

            Collections.shuffle(bulk);
        }

        private static byte[] getRandomBytes(int len) {
            byte[] bytes = new byte[len];
            RANDOM.nextBytes(bytes);
            return bytes;
        }

        public static ResourceSpans getResourceSpans(final String serviceName, final String spanName, final byte[]
                spanId, final byte[] parentId, final byte[] traceId, final Span.SpanKind spanKind) {
            final ByteString parentSpanId = parentId != null ? ByteString.copyFrom(parentId) : ByteString.EMPTY;
            return ResourceSpans.newBuilder()
                    .setResource(
                            Resource.newBuilder()
                                    .addAttributes(KeyValue.newBuilder()
                                            .setKey("service.name")
                                            .setValue(AnyValue.newBuilder().setStringValue(serviceName).build()).build())
                                    .build()
                    )
                    .addInstrumentationLibrarySpans(
                            0,
                            InstrumentationLibrarySpans.newBuilder()
                                    .addSpans(
                                            Span.newBuilder()
                                                    .setName(spanName)
                                                    .setKind(spanKind)
                                                    .setSpanId(ByteString.copyFrom(spanId))
                                                    .setParentSpanId(parentSpanId)
                                                    .setTraceId(ByteString.copyFrom(traceId))
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();
        }

        public static ExportTraceServiceRequest getExportTraceServiceRequest(ResourceSpans...spans){
            return ExportTraceServiceRequest.newBuilder()
                    .addAllResourceSpans(Arrays.asList(spans))
                    .build();
        }
    }

    @Benchmark
    @Fork(value = 1)
    @Warmup(iterations = 2)
    @OperationsPerInvocation(value = OPERATIONS_PER_INVOCATION)
    public void benchmarkExecute(OtelTraceRawPrepperState otelTraceRawPrepperState, TestDataState testDataState) {
        for (int i = 0; i < OPERATIONS_PER_INVOCATION; i++) {
            otelTraceRawPrepperState.otelTraceRawPrepper.execute(testDataState.bulk.subList(i*testDataState.batchSize, (i+1)*testDataState.batchSize));
        }
    }
}
