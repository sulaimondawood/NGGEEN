package com.dawood.nggeen.trade.infrastructure.persistence;

import com.dawood.nggeen.trade.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TradeRepository extends JpaRepository<Trade, UUID> {
}
