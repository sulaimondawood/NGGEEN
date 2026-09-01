package com.dawood.nggeen.shared.infrastructure.persistence;

import com.dawood.nggeen.shared.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
}
