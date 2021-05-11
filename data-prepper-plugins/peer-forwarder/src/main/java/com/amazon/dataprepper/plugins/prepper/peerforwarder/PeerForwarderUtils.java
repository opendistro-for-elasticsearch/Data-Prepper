package com.amazon.dataprepper.plugins.prepper.peerforwarder;

import com.linecorp.armeria.internal.shaded.bouncycastle.util.encoders.Hex;
import io.opentelemetry.proto.trace.v1.InstrumentationLibrarySpans;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Span;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PeerForwarderUtils {
    public static int getResourceSpansSize(final ResourceSpans rs) {
        return rs.getInstrumentationLibrarySpansList().stream().mapToInt(InstrumentationLibrarySpans::getSpansCount).sum();
    }

    public static List<Map.Entry<String, ResourceSpans>> splitByTrace(final ResourceSpans rs) {
        final List<Map.Entry<String, ResourceSpans>> result = new ArrayList<>();
        for (final InstrumentationLibrarySpans ils: rs.getInstrumentationLibrarySpansList()) {
            final Map<String, ResourceSpans.Builder> batches = new HashMap<>();
            for (final Span span: ils.getSpansList()) {
                final String sTraceId = Hex.toHexString(span.getTraceId().toByteArray());
                if (!batches.containsKey(sTraceId)) {
                    final ResourceSpans.Builder newRSBuilder = ResourceSpans.newBuilder()
                            .setResource(rs.getResource());
                    newRSBuilder.addInstrumentationLibrarySpansBuilder().setInstrumentationLibrary(ils.getInstrumentationLibrary());
                    batches.put(sTraceId, newRSBuilder);
                }

                // there is only one instrumentation library per batch
                batches.get(sTraceId).getInstrumentationLibrarySpansBuilder(0).addSpans(span);
            }

            batches.forEach((traceId, rsBuilder) -> result.add(new AbstractMap.SimpleEntry<>(traceId, rsBuilder.build())));
        }

        return result;
    }
}
