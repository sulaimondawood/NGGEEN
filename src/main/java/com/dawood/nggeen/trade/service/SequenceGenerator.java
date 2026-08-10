package com.dawood.nggeen.trade.service;

import java.util.concurrent.atomic.AtomicLong;

public class SequenceGenerator {
    private final AtomicLong sequence = new AtomicLong(0);

    public long next() {
        return sequence.incrementAndGet();
    }

    public long current() {
        return sequence.get();
    }

}
