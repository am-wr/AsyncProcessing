package perf;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 2, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.SampleTime)
public class WorkingWithQueue {

    @State(Scope.Benchmark)
    public static class StateHolder {
        private AtomicLong counter = new AtomicLong();
        public ArrayBlockingQueue<Long> items = new ArrayBlockingQueue<>(1024);

        @Setup(Level.Invocation)
        public void setUp() {
            long next = counter.incrementAndGet();
            if (next % 2 == 1) {
                items.offer(next);
            }
        }
    }

    @Benchmark
    public Long blocking(StateHolder state) throws InterruptedException {
        Long last;
        while ((last = state.items.poll(1, TimeUnit.NANOSECONDS)) != null) {
        }
        return last;
    }

    @Benchmark
    public Long nonBlocking(StateHolder state) {
        Long last;
        while ((last = state.items.poll()) != null) {
        }
        return last;
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .jvmArgs("-Xmn128m")
                .include(WorkingWithQueue.class.getSimpleName())
                .addProfiler(GCProfiler.class)
                .build();

        new Runner(opt).run();
    }
}

/*

# JMH version: 1.21
# VM version: JDK 1.8.0_222, OpenJDK 64-Bit Server VM, 25.222-b10
# VM invoker: /Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home/jre/bin/java
# VM options: -Xmn128m
# Warmup: 2 iterations, 2 s each
# Measurement: 3 iterations, 3 s each
# Timeout: 10 min per iteration
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Sampling time
# Benchmark: perf.WorkingWithQueue.blocking

 Benchmark                                                        Mode     Cnt      Score     Error   Units
 WorkingWithQueue.blocking                                      sample  259723    224.647 ±   4.617   ns/op
 WorkingWithQueue.blocking:blocking·p0.00                       sample            189.000             ns/op
 WorkingWithQueue.blocking:blocking·p0.50                       sample            198.000             ns/op
 WorkingWithQueue.blocking:blocking·p0.90                       sample            204.000             ns/op
 WorkingWithQueue.blocking:blocking·p0.95                       sample            208.000             ns/op
 WorkingWithQueue.blocking:blocking·p0.99                       sample            270.000             ns/op
 WorkingWithQueue.blocking:blocking·p0.999                      sample           8920.832             ns/op
 WorkingWithQueue.blocking:blocking·p0.9999                     sample          31616.883             ns/op
 WorkingWithQueue.blocking:blocking·p1.00                       sample          86400.000             ns/op
 WorkingWithQueue.blocking:·gc.alloc.rate                       sample       3    132.260 ±   9.629  MB/sec
 WorkingWithQueue.blocking:·gc.alloc.rate.norm                  sample       3     44.021 ±   0.029    B/op
 WorkingWithQueue.blocking:·gc.churn.PS_Eden_Space              sample       3    126.679 ± 363.540  MB/sec
 WorkingWithQueue.blocking:·gc.churn.PS_Eden_Space.norm         sample       3     42.161 ± 120.529    B/op
 WorkingWithQueue.blocking:·gc.churn.PS_Survivor_Space          sample       3      0.249 ±   6.071  MB/sec
 WorkingWithQueue.blocking:·gc.churn.PS_Survivor_Space.norm     sample       3      0.083 ±   2.011    B/op
 WorkingWithQueue.blocking:·gc.count                            sample       3     11.000            counts
 WorkingWithQueue.blocking:·gc.time                             sample       3      7.000                ms

 WorkingWithQueue.nonBlocking                                   sample  274524     64.183 ±   1.948   ns/op
 WorkingWithQueue.nonBlocking:nonBlocking·p0.00                 sample             16.000             ns/op
 WorkingWithQueue.nonBlocking:nonBlocking·p0.50                 sample             60.000             ns/op
 WorkingWithQueue.nonBlocking:nonBlocking·p0.90                 sample             63.000             ns/op
 WorkingWithQueue.nonBlocking:nonBlocking·p0.95                 sample             67.000             ns/op
 WorkingWithQueue.nonBlocking:nonBlocking·p0.99                 sample             76.000             ns/op
 WorkingWithQueue.nonBlocking:nonBlocking·p0.999                sample            445.650             ns/op
 WorkingWithQueue.nonBlocking:nonBlocking·p0.9999               sample          14251.800             ns/op
 WorkingWithQueue.nonBlocking:nonBlocking·p1.00                 sample          47488.000             ns/op
 WorkingWithQueue.nonBlocking:·gc.alloc.rate                    sample       3     76.406 ±  11.297  MB/sec
 WorkingWithQueue.nonBlocking:·gc.alloc.rate.norm               sample       3     12.009 ±   0.002    B/op
 WorkingWithQueue.nonBlocking:·gc.churn.PS_Eden_Space           sample       3     73.590 ± 135.662  MB/sec
 WorkingWithQueue.nonBlocking:·gc.churn.PS_Eden_Space.norm      sample       3     11.562 ±  19.828    B/op
 WorkingWithQueue.nonBlocking:·gc.churn.PS_Survivor_Space       sample       3      0.205 ±   6.187  MB/sec
 WorkingWithQueue.nonBlocking:·gc.churn.PS_Survivor_Space.norm  sample       3      0.032 ±   0.981    B/op
 WorkingWithQueue.nonBlocking:·gc.count                         sample       3      7.000            counts
 WorkingWithQueue.nonBlocking:·gc.time                          sample       3      7.000                ms

 */