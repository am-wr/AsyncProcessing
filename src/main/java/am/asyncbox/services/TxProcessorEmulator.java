package am.asyncbox.services;

import am.asyncbox.core.ProcessingService;
import am.asyncbox.model.TransactionDto;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

// This part is simulation of payment module.
// That module will be using Kafka topics - one for receiving payment requests, another to send data back. ID - transaction ID
// In this example I have DTO (with id) in the response queue, it real app we will have k-v pair instead.
@SuppressWarnings("Convert2MethodRef")
@Slf4j
@RequiredArgsConstructor
public class TxProcessorEmulator implements Closeable {

    @NonNull
    private final Queue<Long> processingRequests;
    @NonNull
    private final Queue<TransactionDto> processingResponses;

    private ProcessingService<Long> txProcessor;

    public static TxProcessorEmulator createStarted(Queue<Long> processingRequests, Queue<TransactionDto> processingResponses) {
        TxProcessorEmulator txProcessorEmulator = new TxProcessorEmulator(processingRequests, processingResponses);
        txProcessorEmulator.start();
        return txProcessorEmulator;
    }

    public void start() {
        this.txProcessor = ProcessingService.ofSize(
                "tx-processing", 5,
                () -> processingRequests.poll(),
                (txId) -> this.process(txId));
        this.txProcessor.start();
    }

    private final Random r = new Random();

    private void process(Long nextId) {
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
        if (this.txProcessor != null) {
            this.txProcessor.close();
        }
    }
}