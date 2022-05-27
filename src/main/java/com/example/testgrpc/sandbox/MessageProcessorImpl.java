package com.example.testgrpc.sandbox;

import com.example.testgrpc.sqs.MessageProcessor;

import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.services.sqs.model.Message;

@Component
@Slf4j
public class MessageProcessorImpl implements MessageProcessor {
    @Override
    public Flux<Message> process(Flux<Message> messageFlux) {
        return messageFlux.subscribeOn(Schedulers.boundedElastic()).doOnNext(message -> {
            try {
                TimeUnit.SECONDS.sleep(2);
                log.info("Message body: {}", message.body());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
