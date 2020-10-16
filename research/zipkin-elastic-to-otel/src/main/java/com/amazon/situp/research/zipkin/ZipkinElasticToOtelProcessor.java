package com.amazon.situp.research.zipkin;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.StringValue;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.InstrumentationLibrarySpans;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class ZipkinElasticToOtelProcessor {
    public static String SPAN_ID = "id";
    public static String TRACE_ID = "traceId";
    public static String TIME_STAMP = "timestamp";
    public static String DURATION = "duration";
    public static String SPAN_KIND = "kind";
    public static String PARENT_ID = "parentId";
    public static String LOCAL_ENDPOINT = "localEndpoint";
    public static String SERVICE_NAME = "serviceName";

    public static Span sourceToSpan(final Map<String, Object> source) {
        final String traceID = (String) source.get(TRACE_ID);
        final String spanID = (String) source.get(SPAN_ID);
        final Long startTime = (Long) source.get(TIME_STAMP);
        final Long duration = Long.valueOf((Integer) source.get(DURATION));
        final long endTime = startTime + duration;
        final String parentID = (String) source.get(PARENT_ID);
        final String spanKind = (String) source.get(SPAN_KIND);
        final Span.Builder spanBuilder = Span.newBuilder()
                .setStartTimeUnixNano(startTime)
                .setEndTimeUnixNano(endTime);
        if (traceID != null) {
            spanBuilder.setTraceId(ByteString.copyFromUtf8(traceID));
        }
        if (spanID != null) {
            spanBuilder.setSpanId(ByteString.copyFromUtf8(spanID));
        }
        if (parentID != null) {
            spanBuilder.setParentSpanId(ByteString.copyFromUtf8(parentID));
        }
        if (spanKind != null) {
            spanBuilder.setKind(Span.SpanKind.valueOf(spanKind));
        }

        return spanBuilder.build();
    }

    public static ExportTraceServiceRequest sourcesToRequest(final List<Map<String, Object>> sources) {
        final ExportTraceServiceRequest.Builder exportTraceServiceRequestBuilder = ExportTraceServiceRequest.newBuilder();
        final Map<String, List<Map<String, Object>>> sourceByService = groupSourceByService(sources);
        for (final Map.Entry<String, List<Map<String, Object>>> entry : sourceByService.entrySet()) {
            final String serviceName = entry.getKey();
            final List<Map<String, Object>> sourceGroup = entry.getValue();
            final ResourceSpans.Builder rsBuilder = ResourceSpans.newBuilder().setResource(Resource.newBuilder()
                    .addAttributes(KeyValue.newBuilder()
                            .setKey(SERVICE_NAME)
                            .setValue(AnyValue.newBuilder().setStringValue(serviceName))
                            .build()
                    )
                    .build()
            );
            final InstrumentationLibrarySpans.Builder isBuilder =
                    InstrumentationLibrarySpans.newBuilder();
            final List<Span> spanGroup = sourceGroup.stream()
                    .map(ZipkinElasticToOtelProcessor::sourceToSpan).collect(Collectors.toList());
            isBuilder.addAllSpans(spanGroup);
            rsBuilder.addInstrumentationLibrarySpans(isBuilder);
            exportTraceServiceRequestBuilder.addResourceSpans(rsBuilder);
        }

        return exportTraceServiceRequestBuilder.build();
    }

    public static Map<String, List<Map<String, Object>>> groupSourceByService(final List<Map<String, Object>> sources) {
        final Map<String, List<Map<String, Object>>> sourceByService = new HashMap<>();
        for (final Map<String, Object> source: sources) {
            String serviceName = null;
            final Map<String, Object> localEndpoint = (Map<String, Object>) source.get(LOCAL_ENDPOINT);
            if (localEndpoint != null) {
                serviceName = (String) localEndpoint.get(SERVICE_NAME);
            }
            if (sourceByService.containsKey(serviceName)) {
                sourceByService.get(serviceName).add(source);
            } else {
                sourceByService.put(serviceName, new ArrayList<>(Arrays.asList(source)));
            }
        }
        return sourceByService;
    }
}