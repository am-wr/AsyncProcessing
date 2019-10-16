package am.asyncbox.controllers;

import am.asyncbox.model.TransactionDto;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;

@Slf4j
@RestController
public class AsyncProcessingController implements Closeable {
    private final ExecutorService execPool = Executors.newFixedThreadPool(1, new TFactory("controller-pool"));
    private Future<?> task;
    private final NonBlockingHashMapLong<CompletableFuture<TransactionDto>> inProgressTxs = new NonBlockingHashMapLong<CompletableFuture<TransactionDto>>();
    private final AtomicLong ids = new AtomicLong();
    private volatile boolean continueProcessing;
    private final long blockingTime = TimeUnit.MICROSECONDS.toNanos(500);

    private final Queue<Long> processingRequests = new ArrayBlockingQueue<>(1024 * 32);
    private final Queue<TransactionDto> processingResponses = new ArrayBlockingQueue<>(1024 * 32);
    private TxProcessorEmulator txEmulator;

    @PostConstruct
    void init() {
        this.txEmulator = new TxProcessorEmulator(processingRequests, processingResponses);
        this.txEmulator.start();

        this.continueProcessing = true;
        Future<?> task = execPool.submit(this::responseProcessorLoop);
    }

    @RequestMapping(path = "/api/transactions", method = RequestMethod.POST)
    public Future<TransactionDto> initiateTransaction() {
        long txId = nextId();
        log.info("Initiating new transaction [{}]", txId);

        CompletableFuture<TransactionDto> response = new CompletableFuture<>();
        inProgressTxs.put(txId, response);
        processingRequests.add(txId);

        log.info("Transaction submitted, returning Future for [{}]", txId);
        return response;
    }

    private void responseProcessorLoop() {
        try {
            while (continueProcessing) {
                TransactionDto nextResponse = processingResponses.poll();
                if (nextResponse != null) {
                    do {
                        try {
                            //todo: measure
                            process(nextResponse);
                        } catch (Exception e) {
                            log.error("Exception while processing", e);
                        }
                        nextResponse = processingResponses.poll();
                    } while (nextResponse != null);
                }
                LockSupport.parkNanos(blockingTime);
            }
            log.info("Exiting tasks processing");
        } catch (Throwable e) {
            log.error("Severe Exception while performing response processing", e);
        }
    }

    private void process(TransactionDto nextResponse) {
        CompletableFuture<TransactionDto> response = inProgressTxs.remove(nextResponse.getId());
        // safety net
        if (response == null) {
            log.error("Something went wrong for [{}]", nextResponse);
            return;
        }

        response.complete(nextResponse);
    }

    private long nextId() {
        return ids.incrementAndGet();
    }

    @Override
    public void close() throws IOException {
        this.continueProcessing = false;
        Future<?> task = this.task;
        if (task != null) {
            this.task = null;
            task.cancel(true);
        }
        this.execPool.shutdownNow();
        TxProcessorEmulator txEmulator = this.txEmulator;
        if (txEmulator != null) {
            this.txEmulator = null;
            txEmulator.close();
        }
    }
}


@Slf4j
@RequiredArgsConstructor
class TxProcessorEmulator implements Closeable {
    private final int workers = 100;
    private final ExecutorService execPool = Executors.newFixedThreadPool(workers, new TFactory("tx-proc-pool"));
    private volatile boolean continueProcessing;
    private final List<Future<?>> tasks = new ArrayList<>(workers);
    private final long blockingTime = TimeUnit.MICROSECONDS.toNanos(500);

    @NonNull
    private final Queue<Long> processingRequests;
    @NonNull
    private final Queue<TransactionDto> processingResponses;

    public void start() {
        this.continueProcessing = true;
        IntStream.range(0, workers).forEach(i -> {
            Future<?> task = execPool.submit(this::processingLoop);
            tasks.add(task);
        });
    }

    @SneakyThrows
    public void stop() {
        this.close();
    }

    private void processingLoop() {
        try {
            while (continueProcessing) {
                Long nextTx = processingRequests.poll();
                if (nextTx != null) {
                    do {
                        try {
                            //todo: measure
                            process(nextTx);
                        } catch (Exception e) {
                            log.error("Exception while processing", e);
                        }
                        nextTx = processingRequests.poll();
                    } while (nextTx != null);
                }
                LockSupport.parkNanos(blockingTime);
            }
            log.info("Exiting tasks processing");
        } catch (Throwable e) {
            log.error("Severe Exception while performing processing", e);
        }
    }

    private final Random r = new Random();

    private void process(long nextId) {
        // Simulate delay
        LockSupport.parkNanos(
//                (Math.abs(r.nextLong()) % TimeUnit.MILLISECONDS.toNanos(1000)) + //0-50 variable
                TimeUnit.MILLISECONDS.toNanos(100)); // 100ms constant

        // construct response
        TransactionDto response = TransactionDto.builder()
                .id(nextId)
                .amount(Math.abs(r.nextLong() % 1000) + 100)
                .currency(1)
                .description("Test transaction [" + nextId + "]")
                .toAccount("Test Account")
                .build();

        //todo: refactor this dirty part (preserving non-blocking approach)
        while (!processingResponses.offer(response)) {
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
        execPool.shutdownNow();
    }
}

@RequiredArgsConstructor
class TFactory implements ThreadFactory {
    private final String prefix;
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, prefix + "-" + threadNumber.getAndIncrement());
        if (t.isDaemon())
            t.setDaemon(false);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }
}
