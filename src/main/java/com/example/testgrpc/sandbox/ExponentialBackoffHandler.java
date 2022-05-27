package com.example.testgrpc.sandbox;

import com.example.testgrpc.sqs.BackoffHandler;

import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lombok.SneakyThrows;

@Component
public class ExponentialBackoffHandler implements BackoffHandler {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile int timeout = 0;

    @Override
    @SneakyThrows
    public void join() {
        var readLock = lock.readLock();
        readLock.lock();
        try {
            if (timeout != 0) {
                TimeUnit.MILLISECONDS.sleep(timeout);
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void onError() {
        var writeLock = lock.writeLock();
        writeLock.lock();
        try {
            if (timeout != 0) {
                timeout*=2;
            } else {
                timeout = 200;
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void onSuccess() {
        var writeLock = lock.writeLock();
        writeLock.lock();
        try {
            if (timeout != 200) {
                timeout/=2;
            } else {
                timeout = 0;
            }
        } finally {
            writeLock.unlock();
        }
    }
}
