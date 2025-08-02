package com.github.observant_sun.rattlegram.util;

import javafx.util.Subscription;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;


public class SimplePublisher<T> {
    private final AtomicLong nextKey = new AtomicLong(0);
    private final Map<Long, Consumer<? super T>> listeners;

    public SimplePublisher() {
        this.listeners = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    public SimplePublisher(List<Consumer<? super T>> listeners) {
        this();
        for (Consumer<? super T> consumer : listeners) {
            register(consumer);
        }
    }

    public Subscription subscribe(Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer);
        long key = register(consumer);
        return new Subscription() {

            @Override
            public void unsubscribe() {
                unregister(key);
            }
        };
    }

    private long register(Consumer<? super T> consumer) {
        long key = nextKey.getAndIncrement();
        listeners.put(key, consumer);
        return key;
    }

    private void unregister(long key) {
        listeners.remove(key);
    }

    public void submit(T t) {
        ExecutorService executorService = ForkJoinPool.commonPool();
        synchronized (listeners) {
            for (Consumer<? super T> consumer : listeners.values()) {
                executorService.execute(() -> consumer.accept(t));
            }
        }
    }
}
