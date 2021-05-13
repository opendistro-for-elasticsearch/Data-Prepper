package com.amazon.dataprepper.server;

import com.amazon.dataprepper.pipeline.server.CloudWatchMeterRegistryProvider;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class CloudWatchMeterRegistryProviderTest {
    private static final String TEST_CLOUDWATCH_PROPERTIES = "cloudwatch_test.properties";

    @Mock
    CloudWatchAsyncClient cloudWatchAsyncClient;

    @Test(expected = NullPointerException.class)
    public void testCreateWithInvalidPropertiesFile() {
        new CloudWatchMeterRegistryProvider("does not exist", cloudWatchAsyncClient);
    }

    @Test
    public void testCreateCloudWatchMeterRegistry() {
        final CloudWatchMeterRegistryProvider cloudWatchMeterRegistryProvider = new CloudWatchMeterRegistryProvider(
                TEST_CLOUDWATCH_PROPERTIES, cloudWatchAsyncClient);
        final CloudWatchMeterRegistry cloudWatchMeterRegistry = cloudWatchMeterRegistryProvider.getCloudWatchMeterRegistry();
        assertThat(cloudWatchMeterRegistry, notNullValue());
    }

}
