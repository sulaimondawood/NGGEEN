package com.dawood.nggeen.trade.event;

import java.time.Instant;

public interface DomainEvent {
    long sequenceNo();
    Instant timestamp();
}