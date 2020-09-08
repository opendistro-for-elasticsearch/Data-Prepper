package com.amazon.situp.plugins.buffer;

import com.amazon.situp.model.buffer.Buffer;
import com.amazon.situp.model.configuration.PluginSetting;
import com.amazon.situp.plugins.PluginException;
import org.junit.Test;

import java.util.HashMap;

import static com.amazon.situp.plugins.PluginFactoryTests.NON_EXISTENT_EMPTY_CONFIGURATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

@SuppressWarnings("rawtypes")
public class BufferFactoryTests {

    /**
     * Tests if BufferFactory is able to retrieve default Source plugins by name
     */
    @Test
    public void testNewBufferClassByNameThatExists() {
        final PluginSetting unboundedBufferConfiguration = new PluginSetting("unbounded-inmemory", new HashMap<>());
        final Buffer actualBuffer = BufferFactory.newBuffer(unboundedBufferConfiguration);
        final Buffer expectedBuffer = new UnboundedInMemoryBuffer();
        assertNotNull(actualBuffer);
        assertEquals(expectedBuffer.getClass().getSimpleName(), actualBuffer.getClass().getSimpleName());
    }

    /**
     * Tests if BufferFactory fails with correct Exception when queried for a non-existent plugin
     */
    @Test
    public void testNonExistentSinkRetrieval() {
        assertThrows(PluginException.class, () -> BufferFactory.newBuffer(NON_EXISTENT_EMPTY_CONFIGURATION));
    }
}
