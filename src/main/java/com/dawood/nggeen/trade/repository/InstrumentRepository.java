package com.dawood.nggeen.trade.repository;

import com.dawood.nggeen.trade.enums.InstrumentStatus;
import com.dawood.nggeen.trade.model.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstrumentRepository extends JpaRepository<Instrument, Long> {
    List<Instrument> findByStatus(InstrumentStatus status);
}
