package am.asyncbox.controllers;

import am.asyncbox.core.ProcessingService;
import am.asyncbox.model.TransactionDto;
import am.asyncbox.services.TxProcessorEmulator;
import lombok.extern.slf4j.Slf4j;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.Closeable;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("Convert2MethodRef")
@Slf4j
@Profile("with-webflux")
@RestController
public class AsyncProcessingWebFluxController implements Closeable {
    private final AtomicLong ids = new AtomicLong();

    private final NonBlockingHashMapLong<CompletableFuture<TransactionDto>> inProgressTxs = new NonBlockingHashMapLong<CompletableFuture<TransactionDto>>();
    private final Queue<Long> processingRequests = new ArrayBlockingQueue<>(1024 * 32);
    private final Queue<TransactionDto> processingResponses = new ArrayBlockingQueue<>(1024 * 32);

    @RequestMapping(path = "/api/transactions", method = RequestMethod.POST)
    public Mono<TransactionDto> initiateTransaction() {
        long txId = nextId();
        log.info("Initiating new transaction [{}]", txId);

        CompletableFuture<TransactionDto> response = new CompletableFuture<>();
        inProgressTxs.put(txId, response);
        processingRequests.add(txId);

        log.info("Transaction submitted, returning Future for [{}]", txId);
        return Mono.fromCompletionStage(response);
    }

    private long nextId() {
        return ids.incrementAndGet();
    }

    //--------------------------------------------------------------------------------------------------
    // in separate thread pool we are waiting for events from queue (or kafka)

    private ProcessingService responseFinisher;

    @PostConstruct
    void initAsyncResponseProcessing() {
        this.responseFinisher = ProcessingService.ofSize(
                "rest-controller", 1,
                () -> processingResponses.poll(),
                (tx) -> this.process(tx));
        this.responseFinisher.start();
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

    //--------------------------------------------------------------------------------------------------
    // Payment module emulator

    private TxProcessorEmulator txEmulator;

    @PostConstruct
    void initTxEmulator() {
        this.txEmulator = TxProcessorEmulator.createStarted(processingRequests, processingResponses);
    }

    //--------------------------------------------------------------------------------------------------
    // Cleanup, it is not important
    @Override
    public void close() throws IOException {
        this.responseFinisher.close();
        this.txEmulator.close();
    }
}