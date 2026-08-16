package com.dawood.nggeen.trade.service.contracts;

import com.dawood.nggeen.trade.model.OrderBookSnapshot;

import java.util.Optional;

public interface SnapshotStore {
    void save(OrderBookSnapshot snapshot);
    Optional<OrderBookSnapshot> loadLatest(String symbol);
}
