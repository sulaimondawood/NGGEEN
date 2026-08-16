package com.dawood.nggeen.trade.service.contracts;

import com.dawood.nggeen.trade.model.OrderBookSnapshot;

public interface SnapshotStore {
    void save(OrderBookSnapshot snapshot);

}
