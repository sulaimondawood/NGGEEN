package com.dawood.nggeen.trade.infrastructure.persistence;

import com.dawood.nggeen.trade.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}
