package com.amazon.dataprepper.model.configuration;

import java.util.Map;

public class PluginSetting {

    private final String name;
    private final Map<String, Object> settings;
    private int processWorkers;
    private String pipelineName;

    public PluginSetting(final String name, final Map<String, Object> settings) {
        this.name = name;
        this.settings = settings;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    /**
     * Returns the number of process workers the pipeline is using; This is only required for special plugin use-cases
     * where plugin implementation depends on the number of process workers. For example, Trace analytics service map
     * prepper plugin uses memory mapped databases and it requires to know the number of workers that operate on it
     * concurrently.
     * @return Number of process workers
     */
    public int getNumberOfProcessWorkers() {
        return processWorkers;
    }

    /**
     * This method is solely for pipeline execution to set the process workers and it is recommended not to be used.
     * @param processWorkers number of process workers
     */
    public void setProcessWorkers(final int processWorkers) {
        this.processWorkers = processWorkers;
    }

    /**
     * Returns the name of the associated pipeline.
     * @return name of the associated pipeline
     */
    public String getPipelineName() {
        return this.pipelineName;
    }

    /**
     * This method is solely for pipeline execution to set the associated pipeline name and it is recommended not to be
     * used.
     * @param pipelineName associated pipeline name
     */
    public void setPipelineName(final String pipelineName) {
        this.pipelineName = pipelineName;
    }

    /**
     * Returns the value of the specified attribute, or null if this settings contains no value for the attribute.
     *
     * @param attribute name of the attribute
     * @return value of the attribute from the metadata
     */
    public Object getAttributeFromSettings(final String attribute) {
        return settings == null ? null : settings.get(attribute);
    }

    /**
     * Returns the value of the specified attribute, or {@code defaultValue} if this settings contains no value for
     * the attribute.
     *
     * @param attribute    name of the attribute
     * @param defaultValue default value for the setting
     * @return the value of the specified attribute, or {@code defaultValue} if this settings contains no value for
     * the attribute
     */
    public Object getAttributeOrDefault(final String attribute, final Object defaultValue) {
        return settings == null ? defaultValue : settings.getOrDefault(attribute, defaultValue);
    }

    /**
     * Returns the value of the specified attribute as integer, or {@code defaultValue} if this settings contains no
     * value for the attribute.
     *
     * @param attribute    name of the attribute
     * @param defaultValue default value for the setting
     * @return the value of the specified attribute, or {@code defaultValue} if this settings contains no value for
     * the attribute. If the value is null, null will be returned.
     */
    public Integer getIntegerOrDefault(final String attribute, final int defaultValue) {
        Object object = getAttributeOrDefault(attribute, defaultValue);
        if (object == null) {
            return null;
        } else if (object instanceof Number) {
            return ((Number) object).intValue();
        } else if (object instanceof String) {
            return Integer.valueOf(String.valueOf(object));
        }

        throw new IllegalArgumentException("Unexpected type for attribute: " + attribute);
    }

    /**
     * Returns the value of the specified attribute as String, or {@code defaultValue} if this settings contains no
     * value for the attribute. If the value is null, null will be returned.
     *
     * @param attribute    name of the attribute
     * @param defaultValue default value for the setting
     * @return the value of the specified attribute, or {@code defaultValue} if this settings contains no value for
     * the attribute. If the value is null, null will be returned.
     */
    public String getStringOrDefault(final String attribute, final String defaultValue) {
        Object object = getAttributeOrDefault(attribute, defaultValue);
        if (object == null) {
            return null;
        } else if (object instanceof String) {
            return String.valueOf(object);
        }

        throw new IllegalArgumentException("Unexpected type for attribute: " + attribute);
    }

    /**
     * Returns the value of the specified attribute as boolean, or {@code defaultValue} if this settings contains no
     * value for the attribute. If the value is null, null will be returned.
     *
     * @param attribute    name of the attribute
     * @param defaultValue default value for the setting
     * @return the value of the specified attribute, or {@code defaultValue} if this settings contains no value for
     * the attribute
     */
    public Boolean getBooleanOrDefault(final String attribute, final boolean defaultValue) {
        Object object = getAttributeOrDefault(attribute, defaultValue);
        if (object == null) {
            return null;
        } else if (object instanceof Boolean) {
            return (Boolean) object;
        } else if (object instanceof String) {
            return Boolean.valueOf(String.valueOf(object));
        }

        throw new IllegalArgumentException("Unexpected type for attribute: " + attribute);
    }

    /**
     * Returns the value of the specified attribute as long, or {@code defaultValue} if this settings contains no
     * value for the attribute. If the value is null, null will be returned.
     *
     * @param attribute    name of the attribute
     * @param defaultValue default value for the setting
     * @return the value of the specified attribute, or {@code defaultValue} if this settings contains no value for
     * the attribute
     */
    public Long getLongOrDefault(final String attribute, final long defaultValue) {
        Object object = getAttributeOrDefault(attribute, defaultValue);
        if (object == null) {
            return null;
        } else if (object instanceof Number) {
            return ((Number) object).longValue();
        } else if (object instanceof String) {
            return Long.valueOf(String.valueOf(object));
        }

        throw new IllegalArgumentException("Unexpected type for attribute: " + attribute);
    }

}
