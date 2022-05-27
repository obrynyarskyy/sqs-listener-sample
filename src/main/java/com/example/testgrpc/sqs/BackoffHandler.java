package com.example.testgrpc.sqs;

public interface BackoffHandler {
    void join();
    void onError();
    void onSuccess();
}
