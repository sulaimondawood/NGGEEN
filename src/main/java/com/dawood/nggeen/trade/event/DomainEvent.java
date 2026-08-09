package com.dawood.nggeen.trade.event;

import java.time.Instant;
import java.util.UUID;

 public interface DomainEvent {
     UUID orderId();
     long sequenceNo();
     String symbol();
     Instant timestamp();
}