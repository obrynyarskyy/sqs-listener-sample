package com.example.testgrpc.sqs;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;

import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@RequiredArgsConstructor
@Service
@Slf4j
public class Subscriber {
    protected static final String QUEUE =
            "https://sqs.eu-central-1.amazonaws.com/134725774482/my-queue";
    private final SqsClient sqsAsyncClient;
    private final MessageProcessor messageProcessor;
    private final BackoffHandler backoffHandler;
    private Disposable disposable;
    private ExecutorService executorService;

    @PostConstruct
    @SneakyThrows
    public void subscribe() {
        executorService = Executors.newFixedThreadPool(1);
        disposable = messageProcessor.process(Flux.generate(() -> backoffHandler, this::generate)
                        .doOnNext(messages -> log.info("Polled {} messages", messages.size()))
                        .flatMapIterable(Function.identity()))
                .subscribeOn(Schedulers.fromExecutor(executorService))
                .buffer(1).subscribe(this::onNext, this::onError);
    }

    private BackoffHandler generate(BackoffHandler backoffHandler,
            SynchronousSink<List<Message>> sink) {
        backoffHandler.join();
        try {
            log.info("Polling messages");
            ReceiveMessageRequest messageRequest =
                    ReceiveMessageRequest.builder()
                            .maxNumberOfMessages(10)
                            .waitTimeSeconds(20)
                            .queueUrl(QUEUE).build();
            backoffHandler.onSuccess();
            sink.next(sqsAsyncClient.receiveMessage(messageRequest).messages());
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return backoffHandler;
    }

    private void onNext(List<Message> messages) {
        sqsAsyncClient.deleteMessageBatch(DeleteMessageBatchRequest.builder().queueUrl(QUEUE)
                .entries(messages.stream().map(message -> DeleteMessageBatchRequestEntry.builder()
                                .receiptHandle(message.receiptHandle()).id(message.messageId()).build())
                        .collect(Collectors.toList())).build());
        log.info("{} messages deleted", messages.size());
    }

    private void onError(Throwable t) {
        t.printStackTrace();
    }


    @PreDestroy
    public void close() {
        disposable.dispose();
        executorService.shutdown();
    }
}
