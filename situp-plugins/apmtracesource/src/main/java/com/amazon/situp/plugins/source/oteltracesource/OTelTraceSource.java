package com.amazon.situp.plugins.source.oteltracesource;

import com.amazon.situp.model.PluginType;
import com.amazon.situp.model.annotations.SitupPlugin;
import com.amazon.situp.model.buffer.Buffer;
import com.amazon.situp.model.configuration.PluginSetting;
import com.amazon.situp.model.record.Record;
import com.amazon.situp.model.source.Source;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.grpc.GrpcService;
import com.linecorp.armeria.server.healthcheck.HealthCheckService;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.ExecutionException;

@SitupPlugin(name = "otel_trace_source", type = PluginType.SOURCE)
public class OTelTraceSource implements Source<Record<ExportTraceServiceRequest>> {
    private static final Logger LOG = LoggerFactory.getLogger(OTelTraceSource.class);
    private static final String HEALTH_CHECK_PATH = "/health_check";
    private final OTelTraceSourceConfig oTelTraceSourceConfig;
    private Server server;

    public OTelTraceSource(final PluginSetting pluginSetting) {
        oTelTraceSourceConfig = OTelTraceSourceConfig.buildConfig(pluginSetting);
    }

    @Override
    public void start(Buffer<Record<ExportTraceServiceRequest>> buffer) {
        if (buffer == null) {
            throw new IllegalStateException("Buffer provided is null");
        }
        if (server == null) {
            final ServerBuilder sb = Server.builder();
            sb.service(
                    GrpcService
                            .builder()
                            .addService(new OTelTraceGrpcService(oTelTraceSourceConfig.getRequestTimeoutInMillis(), buffer))
                            .enableUnframedRequests(true) // This will enable non-Grpc requests
                            .useClientTimeoutHeader(false)
                            .build());
            sb.requestTimeoutMillis(oTelTraceSourceConfig.getRequestTimeoutInMillis());
            if (oTelTraceSourceConfig.isSsl()) {
                sb.https(oTelTraceSourceConfig.getPort()).tls(new File(oTelTraceSourceConfig.getSslKeyCertChainFile()),
                        new File(oTelTraceSourceConfig.getSslKeyFile()));
            } else {
                sb.http(oTelTraceSourceConfig.getPort());
            }
            if(oTelTraceSourceConfig.isHealthCheck()) {
                sb.service(HEALTH_CHECK_PATH, HealthCheckService.of());
            }
            //TODO: Expose BlockingTaskExecutor/Connections
            server = sb.build();
        }
        try {
            server.start().get();
        } catch (ExecutionException ex) {
            if (ex.getCause() != null && ex.getCause() instanceof RuntimeException) {
                throw (RuntimeException) ex.getCause();
            } else {
                throw new RuntimeException(ex);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
        LOG.info("Started otel_trace_source...");
    }

    @Override
    public void stop() {
        if (server != null) {
            try {
                server.stop().get();
            } catch (ExecutionException ex) {
                if (ex.getCause() != null && ex.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) ex.getCause();
                } else {
                    throw new RuntimeException(ex);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ex);
            }
        }
        LOG.info("Stopped otel_trace_source.");
    }

    public OTelTraceSourceConfig getoTelTraceSourceConfig() {
        return oTelTraceSourceConfig;
    }
}
