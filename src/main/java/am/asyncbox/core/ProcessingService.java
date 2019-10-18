package am.asyncbox.core;

import am.asyncbox.utils.NamedFactory;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

@Slf4j
@RequiredArgsConstructor
public final class ProcessingService<T> implements Closeable {
    private final int workers;
    @NonNull
    private final ExecutorService execPool;
    private volatile boolean continueProcessing;
    private final List<Future<?>> tasks;

    @NonNull
    private final Supplier<T> inputProvider;
    @NonNull
    private final Consumer<T> processor;

    private final long blockingTime = TimeUnit.MICROSECONDS.toNanos(500);

    public static <T> ProcessingService<T> ofSize(String name, int workers, @NonNull Supplier<T> inputProvider, @NonNull Consumer<T> processor) {
        ExecutorService pool = Executors.newFixedThreadPool(workers, NamedFactory.named(name));
        ArrayList<Future<?>> tasks = new ArrayList<>(workers);
        return new ProcessingService<>(workers, pool, tasks, inputProvider, processor);
    }

    public void start() {
        this.continueProcessing = true;
        IntStream.range(0, workers).forEach(i -> {
            Future<?> task = execPool.submit(this::processingLoop);
            tasks.add(task);
        });
    }

    private void processingLoop() {
        try {
            while (continueProcessing) {
                T next = this.inputProvider.get();
                if (next != null) {
                    do {
                        //todo: measure
                        process(next);

                        next = this.inputProvider.get();
                    } while (next != null);
                }
                LockSupport.parkNanos(blockingTime);
            }
            log.info("Exiting tasks processing");
        } catch (Throwable e) {
            log.error("Severe Exception while performing processing", e);
        }
    }

    private void process(T next) {
        try {
            this.processor.accept(next);
        } catch (Exception e) {
            log.error("Exception while processing", e);
        }
    }

    @Override
    public void close() throws IOException {
        this.continueProcessing = false;
        this.tasks.forEach(t -> {
            try {
                t.cancel(true);
            } catch (Exception e) {
                log.error("Error while closing", e);
            }
        });
        this.execPool.shutdownNow();
    }
}
