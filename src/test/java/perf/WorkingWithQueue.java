package perf;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 2, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 10, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
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
                .jvmArgs("-Xmn2g")
                .shouldDoGC(true)
                .include(WorkingWithQueue.class.getSimpleName())
//                .addProfiler(GCProfiler.class)
                .build();

        new Runner(opt).run();
    }
}

/*

# JMH version: 1.21
# VM version: JDK 1.8.0_222, OpenJDK 64-Bit Server VM, 25.222-b10
# VM invoker: /Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home/jre/bin/java
# VM options: -Xmn2g
# Warmup: 2 iterations, 2 s each
# Measurement: 3 iterations, 10 s each
# Timeout: 10 min per iteration
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Throughput, ops/time
# Benchmark: perf.WorkingWithQueue.blocking

Benchmark                      Mode  Cnt      Score      Error   Units
WorkingWithQueue.blocking     thrpt    3   4930.046 ± 3358.447  ops/ms
WorkingWithQueue.nonBlocking  thrpt    3  18147.775 ± 2359.855  ops/ms

 */