package com.amazon.dataprepper.plugins.prepper.oteltrace.model;

import java.util.HashSet;
import java.util.Set;

public class RawSpanSet {

    private final Set<RawSpan> rawSpans;
    private final long timeSeen;

    public RawSpanSet() {
        this.rawSpans = new HashSet<>();
        this.timeSeen = System.currentTimeMillis();
    }

    public Set<RawSpan> getRawSpans() {
        // return a copy to avoid ConcurrentModificationException
        return new HashSet<>(rawSpans);
    }

    public long getTimeSeen() {
        return timeSeen;
    }

    public void addRawSpan(final RawSpan rawSpan) {
        rawSpans.add(rawSpan);
    }
}