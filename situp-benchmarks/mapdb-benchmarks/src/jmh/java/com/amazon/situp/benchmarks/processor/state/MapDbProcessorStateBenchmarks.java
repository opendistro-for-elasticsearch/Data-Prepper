package com.amazon.situp.benchmarks.processor.state;

import com.amazon.situp.plugins.processor.state.MapDbProcessorState;
import com.google.common.primitives.SignedBytes;
import org.openjdk.jmh.annotations.*;

import java.io.File;
import java.util.*;

@State(Scope.Benchmark)
public class MapDbProcessorStateBenchmarks {
    private static final int BATCH_SIZE = 100;
    private static final int NUM_BATCHES = 10000;
    private static final Random RANDOM = new Random();
    private static final String DB_PATH = "data/benchmark";
    private static final String DB_NAME = "benchmarkDb";

    private MapDbProcessorState<String> mapDbProcessorState;
    private List<Map<byte[], String>> data = new ArrayList<Map<byte[], String>>(){{
        for(int i=0; i<NUM_BATCHES; i++) {
            final TreeMap<byte[], String> batch = new TreeMap<>(SignedBytes.lexicographicalComparator());
            for(int j=0; j<BATCH_SIZE; j++) {
                batch.put(getRandomBytes(), UUID.randomUUID().toString());
            }
            add(batch);
        }
    }};

    private byte[] getRandomBytes() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    @Setup(Level.Iteration)
    public void setup() {
        final File path = new File(DB_PATH);
        if(!path.exists()){
            if(!path.mkdirs()){
                throw new RuntimeException(String.format("Unable to create the directory at the provided path: %s", path.getName()));
            }
        }
        mapDbProcessorState = new MapDbProcessorState<>(new File(DB_PATH), DB_NAME);

    }

    @TearDown(Level.Iteration)
    public void teardown() {
        mapDbProcessorState.delete();
    }

    @Benchmark
    @Fork(value = 1)
    @Warmup(iterations = 3)
    @Threads(value = 2)
    @Measurement(iterations = 5)
    public void benchmarkPutAll() {
        mapDbProcessorState.putAll(data.get(RANDOM.nextInt(NUM_BATCHES)));
    }


}
