package com.github.observant_sun.rattlegram.util;

import javafx.application.Platform;
import javafx.util.Subscription;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class VoidPublisher {
    private final AtomicLong nextKey = new AtomicLong(0);
    private final Map<Long, Runnable> listeners;

    public VoidPublisher() {
        this.listeners = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    public VoidPublisher(List<Runnable> listeners) {
        this();
        for (Runnable runnable : listeners) {
            register(runnable);
        }
    }

    public Subscription subscribe(Runnable runnable) {
        Objects.requireNonNull(runnable);
        long key = register(runnable);
        return new Subscription() {

            @Override
            public void unsubscribe() {
                unregister(key);
            }
        };
    }

    private long register(Runnable runnable) {
        long key = nextKey.getAndIncrement();
        listeners.put(key, runnable);
        return key;
    }

    private void unregister(long key) {
        listeners.remove(key);
    }

    public void publish() {
        ExecutorService executorService = ForkJoinPool.commonPool();
        synchronized (listeners) {
            for (Runnable runnable : listeners.values()) {
                executorService.execute(runnable);
            }
        }
    }
}
