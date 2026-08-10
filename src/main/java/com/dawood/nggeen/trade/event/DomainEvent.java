package com.dawood.nggeen.trade.event;

import net.openhft.chronicle.wire.Marshallable;

import java.time.Instant;

public interface DomainEvent extends Marshallable {
     long sequenceNo();
     String symbol();
     Instant timestamp();
}