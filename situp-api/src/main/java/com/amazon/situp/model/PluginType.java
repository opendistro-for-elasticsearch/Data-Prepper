package com.amazon.situp.model;

import com.amazon.situp.model.buffer.Buffer;
import com.amazon.situp.model.processor.Processor;
import com.amazon.situp.model.sink.Sink;
import com.amazon.situp.model.source.Source;

public enum PluginType {
    SOURCE("source", Source.class),
    BUFFER("buffer", Buffer.class),
    PROCESSOR("processor", Processor.class),
    SINK("sink", Sink.class);

    private final String pluginName;
    private final Class<?> pluginClass;

    PluginType(final String pluginName, final Class<?> pluginClass) {
        this.pluginName = pluginName;
        this.pluginClass = pluginClass;
    }

    public String pluginName() {
        return pluginName;
    }

    public Class<?> pluginClass() {
        return pluginClass;
    }
}
