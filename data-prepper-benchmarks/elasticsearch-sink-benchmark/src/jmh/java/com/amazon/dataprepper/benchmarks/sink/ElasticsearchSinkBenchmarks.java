/*
 * SPDX-License-Identifier: Apache-2.0
 *  *
 *  * The OpenSearch Contributors require contributions made to
 *  * this file be licensed under the Apache-2.0 license or a
 *  * compatible open source license.
 */

package com.amazon.dataprepper.benchmarks.sink;

import com.amazon.dataprepper.model.configuration.PluginSetting;
import com.amazon.dataprepper.model.record.Record;
import com.amazon.dataprepper.plugins.prepper.oteltrace.model.OTelProtoHelper;
import com.amazon.dataprepper.plugins.prepper.oteltrace.model.RawSpanBuilder;
import com.amazon.dataprepper.plugins.sink.elasticsearch.ElasticsearchSink;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ElasticsearchSinkBenchmarks {

    @State(Scope.Benchmark)
    public static class ElasticsearchSinkState {
        private ElasticsearchSink elasticsearchSink;

        @Param(value = "")
        private String endpoint;

        @Setup(Level.Trial)
        public void setupElasticsearchSink() {
            final PluginSetting pluginSetting = new PluginSetting("elasticsearch", new HashMap<String, Object>(){{
                put("hosts", Collections.singletonList(endpoint));
                put("aws_sigv4", true);
                put("trace_analytics_raw", true);
            }}) {{
                setPipelineName("test-pipeline");
            }};

            elasticsearchSink = new ElasticsearchSink(pluginSetting);
        }

        @TearDown(Level.Trial)
        public void shutdownElasticsearchSink() {
            elasticsearchSink.shutdown();
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
        private List<Record<String>> bulk;

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
        public void generateBatch() throws JsonProcessingException {
            final List<Record<ExportTraceServiceRequest>> temp = new ArrayList<>();
            for(int j=0; j<batchSize; j++) {
                final byte[] traceId = getTraceId();
                final byte[] spanId = getSpanId(traceId);
                final byte[] parentId = getParentId(traceId, spanId);
                temp.add(new Record<>(getExportTraceServiceRequest(
                        getResourceSpans(
                                serviceNames.get(RANDOM.nextInt(serviceNames.size())),
                                traceGroups.get(RANDOM.nextInt(traceGroups.size())),
                                spanId,
                                parentId,
                                traceId,
                                Span.SpanKind.SPAN_KIND_CLIENT
                        ))));
            }
            bulk = parseRequests(temp);
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

        public static List<Record<String>> parseRequests(List<Record<ExportTraceServiceRequest>> records) throws JsonProcessingException {
            final List<Record<String>> finalRecords = new LinkedList<>();
            for (Record<ExportTraceServiceRequest> ets : records) {
                for (ResourceSpans rs : ets.getData().getResourceSpansList()) {
                    final String serviceName = OTelProtoHelper.getServiceName(rs.getResource()).orElse(null);
                    final Map<String, Object> resourceAttributes = OTelProtoHelper.getResourceAttributes(rs.getResource());
                    for (InstrumentationLibrarySpans is : rs.getInstrumentationLibrarySpansList()) {
                        for (Span sp : is.getSpansList()) {
                            finalRecords.add(new Record<>(new RawSpanBuilder()
                                    .setFromSpan(sp, is.getInstrumentationLibrary(), serviceName, resourceAttributes)
                                    .build().toJson()));
                        }
                    }
                }
            }
            return finalRecords;
        }
    }

    @Benchmark
    @Fork(value = 1)
    @Warmup(iterations = 2)
    public void benchmarkExecute(ElasticsearchSinkState elasticsearchSinkState, TestDataState testDataState) {
        elasticsearchSinkState.elasticsearchSink.output(testDataState.bulk);
    }
}
