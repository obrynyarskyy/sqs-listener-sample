package com.example.testgrpc.sqs;

import reactor.core.publisher.Flux;
import software.amazon.awssdk.services.sqs.model.Message;

public interface MessageProcessor {
    Flux<Message> process(Flux<Message> messageFlux);
}


interface SyncProcessor {

}
