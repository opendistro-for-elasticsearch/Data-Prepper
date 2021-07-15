# Elasticsearch sink raw span Benchmarks

This package contains benchmarks for the elasticsearch sink output on raw trace spans using JMH: https://openjdk.java.net/projects/code-tools/jmh/ . 

Integration with gradle is done with the following gradle plugin for JMH: https://github.com/melix/jmh-gradle-plugin.

The plugin creates a source set for the JMH benchmarks, and provides a few gradle tasks for running and building the benchmarks.

## Configuration

- endpoint: A string represents Amazon Elasticsearch Service endpoint. Required for benchmark.

## Running the tests via gradle task

Tests can be run via the "jmh" gradle task provided by the plugin. The README for the plugin provides the various parameters that
can be provided to the plugin. 

## Running the tests via JAR

To run the tests via JAR, you can build the benchmark jar using the gradle task "shadowJar". This jar is an executable jar 
that runs the benchmark tests. Example command:

```
java -jar elasticsearch-sink-benchmark-0.1-beta-all.jar -p endpoint=https://<your-aes-address>.<region>.es.amazonaws.com 
```

## Results

### single thread

```
java -jar build/libs/elasticsearch-sink-benchmark-0.1-beta-all.jar -p endpoint=https://search-elasticsearch-sink-testing-pdiunswaqztrxksllaw5fr5jb4.us-east-1.es.amazonaws.com
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.openjdk.jmh.util.Utils (file:/Users/qchea/IdeaProjects/simple-ingest-transformation-utility-pipeline/data-prepper-benchmarks/elasticsearch-sink-benchmark/build/libs/elasticsearch-sink-benchmark-0.1-beta-all.jar) to field java.io.Console.cs
WARNING: Please consider reporting this to the maintainers of org.openjdk.jmh.util.Utils
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
# JMH version: 1.25
# VM version: JDK 14.0.1, OpenJDK 64-Bit Server VM, 14.0.1+7
# VM invoker: /Users/qchea/Library/Java/JavaVirtualMachines/openjdk-14.0.1/Contents/Home/bin/java
# VM options: <none>
# Warmup: 2 iterations, 10 s each
# Measurement: 5 iterations, 10 s each
# Timeout: 10 min per iteration
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Throughput, ops/time
# Benchmark: com.amazon.dataprepper.benchmarks.sink.ElasticsearchSinkBenchmarks.benchmarkExecute
# Parameters: (batchSize = 10, endpoint = https://search-elasticsearch-sink-testing-pdiunswaqztrxksllaw5fr5jb4.us-east-1.es.amazonaws.com)

# Run progress: 0.00% complete, ETA 00:01:10
# Fork: 1 of 1
# Warmup Iteration   1: WARNING: sun.reflect.Reflection.getCallerClass is not supported. This will impact performance.
11.337 ops/s
# Warmup Iteration   2: 12.558 ops/s
Iteration   1: 12.820 ops/s
Iteration   2: 13.652 ops/s
Iteration   3: 14.171 ops/s
Iteration   4: 14.398 ops/s
Iteration   5: 14.424 ops/s


Result "com.amazon.dataprepper.benchmarks.sink.ElasticsearchSinkBenchmarks.benchmarkExecute":
  13.893 ±(99.9%) 2.600 ops/s [Average]
  (min, avg, max) = (12.820, 13.893, 14.424), stdev = 0.675
  CI (99.9%): [11.293, 16.493] (assumes normal distribution)


# Run complete. Total time: 00:01:20

REMEMBER: The numbers below are just data. To gain reusable insights, you need to follow up on
why the numbers are the way they are. Use profilers (see -prof, -lprof), design factorial
experiments, perform baseline and negative tests that provide experimental control, make sure
the benchmarking environment is safe on JVM/OS/HW level, ask for reviews from the domain experts.
Do not assume the numbers tell you what you want them to tell.

Benchmark                                     (batchSize)                                                                                       (endpoint)   Mode  Cnt   Score   Error  Units
ElasticsearchSinkBenchmarks.benchmarkExecute           10  https://search-elasticsearch-sink-testing-pdiunswaqztrxksllaw5fr5jb4.us-east-1.es.amazonaws.com  thrpt    5  13.893 ± 2.600  ops/s
```

### 2-thread
```
java -jar build/libs/elasticsearch-sink-benchmark-0.1-beta-all.jar -p endpoint=https://search-elasticsearch-sink-testing-pdiunswaqztrxksllaw5fr5jb4.us-east-1.es.amazonaws.com -t 2
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.openjdk.jmh.util.Utils (file:/Users/qchea/IdeaProjects/simple-ingest-transformation-utility-pipeline/data-prepper-benchmarks/elasticsearch-sink-benchmark/build/libs/elasticsearch-sink-benchmark-0.1-beta-all.jar) to field java.io.Console.cs
WARNING: Please consider reporting this to the maintainers of org.openjdk.jmh.util.Utils
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
# JMH version: 1.25
# VM version: JDK 14.0.1, OpenJDK 64-Bit Server VM, 14.0.1+7
# VM invoker: /Users/qchea/Library/Java/JavaVirtualMachines/openjdk-14.0.1/Contents/Home/bin/java
# VM options: <none>
# Warmup: 2 iterations, 10 s each
# Measurement: 5 iterations, 10 s each
# Timeout: 10 min per iteration
# Threads: 2 threads, will synchronize iterations
# Benchmark mode: Throughput, ops/time
# Benchmark: com.amazon.dataprepper.benchmarks.sink.ElasticsearchSinkBenchmarks.benchmarkExecute
# Parameters: (batchSize = 10, endpoint = https://search-elasticsearch-sink-testing-pdiunswaqztrxksllaw5fr5jb4.us-east-1.es.amazonaws.com)

# Run progress: 0.00% complete, ETA 00:01:10
# Fork: 1 of 1
# Warmup Iteration   1: WARNING: sun.reflect.Reflection.getCallerClass is not supported. This will impact performance.
26.780 ops/s
# Warmup Iteration   2: 26.586 ops/s
Iteration   1: 28.820 ops/s
Iteration   2: 28.983 ops/s
Iteration   3: 29.646 ops/s
Iteration   4: 27.818 ops/s
Iteration   5: 27.873 ops/s


Result "com.amazon.dataprepper.benchmarks.sink.ElasticsearchSinkBenchmarks.benchmarkExecute":
  28.628 ±(99.9%) 2.999 ops/s [Average]
  (min, avg, max) = (27.818, 28.628, 29.646), stdev = 0.779
  CI (99.9%): [25.629, 31.627] (assumes normal distribution)


# Run complete. Total time: 00:01:19

REMEMBER: The numbers below are just data. To gain reusable insights, you need to follow up on
why the numbers are the way they are. Use profilers (see -prof, -lprof), design factorial
experiments, perform baseline and negative tests that provide experimental control, make sure
the benchmarking environment is safe on JVM/OS/HW level, ask for reviews from the domain experts.
Do not assume the numbers tell you what you want them to tell.

Benchmark                                     (batchSize)                                                                                       (endpoint)   Mode  Cnt   Score   Error  Units
ElasticsearchSinkBenchmarks.benchmarkExecute           10  https://search-elasticsearch-sink-testing-pdiunswaqztrxksllaw5fr5jb4.us-east-1.es.amazonaws.com  thrpt    5  28.628 ± 2.999  ops/s
```

### 4-thread

```
 java -jar build/libs/elasticsearch-sink-benchmark-0.1-beta-all.jar -p endpoint=https://search-elasticsearch-sink-testing-pdiunswaqztrxksllaw5fr5jb4.us-east-1.es.amazonaws.com -t 4
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.openjdk.jmh.util.Utils (file:/Users/qchea/IdeaProjects/simple-ingest-transformation-utility-pipeline/data-prepper-benchmarks/elasticsearch-sink-benchmark/build/libs/elasticsearch-sink-benchmark-0.1-beta-all.jar) to field java.io.Console.cs
WARNING: Please consider reporting this to the maintainers of org.openjdk.jmh.util.Utils
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
# JMH version: 1.25
# VM version: JDK 14.0.1, OpenJDK 64-Bit Server VM, 14.0.1+7
# VM invoker: /Users/qchea/Library/Java/JavaVirtualMachines/openjdk-14.0.1/Contents/Home/bin/java
# VM options: <none>
# Warmup: 2 iterations, 10 s each
# Measurement: 5 iterations, 10 s each
# Timeout: 10 min per iteration
# Threads: 4 threads, will synchronize iterations
# Benchmark mode: Throughput, ops/time
# Benchmark: com.amazon.dataprepper.benchmarks.sink.ElasticsearchSinkBenchmarks.benchmarkExecute
# Parameters: (batchSize = 10, endpoint = https://search-elasticsearch-sink-testing-pdiunswaqztrxksllaw5fr5jb4.us-east-1.es.amazonaws.com)

# Run progress: 0.00% complete, ETA 00:01:10
# Fork: 1 of 1
# Warmup Iteration   1: WARNING: sun.reflect.Reflection.getCallerClass is not supported. This will impact performance.
53.437 ops/s
# Warmup Iteration   2: 55.765 ops/s
Iteration   1: 54.300 ops/s
Iteration   2: 52.058 ops/s
Iteration   3: 53.406 ops/s
Iteration   4: 56.153 ops/s
Iteration   5: 54.609 ops/s


Result "com.amazon.dataprepper.benchmarks.sink.ElasticsearchSinkBenchmarks.benchmarkExecute":
  54.105 ±(99.9%) 5.828 ops/s [Average]
  (min, avg, max) = (52.058, 54.105, 56.153), stdev = 1.514
  CI (99.9%): [48.277, 59.934] (assumes normal distribution)


# Run complete. Total time: 00:01:21

REMEMBER: The numbers below are just data. To gain reusable insights, you need to follow up on
why the numbers are the way they are. Use profilers (see -prof, -lprof), design factorial
experiments, perform baseline and negative tests that provide experimental control, make sure
the benchmarking environment is safe on JVM/OS/HW level, ask for reviews from the domain experts.
Do not assume the numbers tell you what you want them to tell.

Benchmark                                     (batchSize)                                                                                       (endpoint)   Mode  Cnt   Score   Error  Units
ElasticsearchSinkBenchmarks.benchmarkExecute           10  https://search-elasticsearch-sink-testing-pdiunswaqztrxksllaw5fr5jb4.us-east-1.es.amazonaws.com  thrpt    5  54.105 ± 5.828  ops/s
```

### 5-thread
```
java -jar build/libs/elasticsearch-sink-benchmark-0.1-beta-all.jar -p endpoint=https://search-elasticsearch-sink-testing-pdiunswaqztrxksllaw5fr5jb4.us-east-1.es.amazonaws.com -t 5
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by org.openjdk.jmh.util.Utils (file:/Users/qchea/IdeaProjects/simple-ingest-transformation-utility-pipeline/data-prepper-benchmarks/elasticsearch-sink-benchmark/build/libs/elasticsearch-sink-benchmark-0.1-beta-all.jar) to field java.io.Console.cs
WARNING: Please consider reporting this to the maintainers of org.openjdk.jmh.util.Utils
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
# JMH version: 1.25
# VM version: JDK 14.0.1, OpenJDK 64-Bit Server VM, 14.0.1+7
# VM invoker: /Users/qchea/Library/Java/JavaVirtualMachines/openjdk-14.0.1/Contents/Home/bin/java
# VM options: <none>
# Warmup: 2 iterations, 10 s each
# Measurement: 5 iterations, 10 s each
# Timeout: 10 min per iteration
# Threads: 5 threads, will synchronize iterations
# Benchmark mode: Throughput, ops/time
# Benchmark: com.amazon.dataprepper.benchmarks.sink.ElasticsearchSinkBenchmarks.benchmarkExecute
# Parameters: (batchSize = 10, endpoint = https://search-elasticsearch-sink-testing-pdiunswaqztrxksllaw5fr5jb4.us-east-1.es.amazonaws.com)

# Run progress: 0.00% complete, ETA 00:01:10
# Fork: 1 of 1
# Warmup Iteration   1: WARNING: sun.reflect.Reflection.getCallerClass is not supported. This will impact performance.
66.398 ops/s
# Warmup Iteration   2: 68.633 ops/s
Iteration   1: 65.337 ops/s
Iteration   2: 68.415 ops/s
Iteration   3: 68.167 ops/s
Iteration   4: 60.479 ops/s
Iteration   5: 69.555 ops/s


Result "com.amazon.dataprepper.benchmarks.sink.ElasticsearchSinkBenchmarks.benchmarkExecute":
  66.391 ±(99.9%) 14.059 ops/s [Average]
  (min, avg, max) = (60.479, 66.391, 69.555), stdev = 3.651
  CI (99.9%): [52.332, 80.449] (assumes normal distribution)


# Run complete. Total time: 00:01:21

REMEMBER: The numbers below are just data. To gain reusable insights, you need to follow up on
why the numbers are the way they are. Use profilers (see -prof, -lprof), design factorial
experiments, perform baseline and negative tests that provide experimental control, make sure
the benchmarking environment is safe on JVM/OS/HW level, ask for reviews from the domain experts.
Do not assume the numbers tell you what you want them to tell.

Benchmark                                     (batchSize)                                                                                       (endpoint)   Mode  Cnt   Score    Error  Units
ElasticsearchSinkBenchmarks.benchmarkExecute           10  https://search-elasticsearch-sink-testing-pdiunswaqztrxksllaw5fr5jb4.us-east-1.es.amazonaws.com  thrpt    5  66.391 ± 14.059  ops/s
```
