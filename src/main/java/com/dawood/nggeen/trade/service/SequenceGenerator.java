package com.dawood.nggeen.trade.service;

import java.util.concurrent.atomic.AtomicLong;

public class SequenceGenerator {
    private AtomicLong sequence;

    public SequenceGenerator() {
        this(0L);
    }

    public SequenceGenerator(long initialSequence) {
        this.sequence = new AtomicLong(initialSequence);
    }

    public long next() {
        return sequence.incrementAndGet();
    }

    public long current() {
        return sequence.get();
    }

}
