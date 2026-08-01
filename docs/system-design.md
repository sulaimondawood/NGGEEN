# NGGEEN
**Crypto Matching Engine – System Design Document**  
Version 1.0

**Project Name:** NGGEEN  
**Type:** Central Limit Order Book (CLOB) Matching Engine for Cryptocurrency Spot Markets  
**Document Purpose:** Complete system design for implementation, learning, and portfolio presentation

-----


## 1. Overview

NGGEEN is a deterministic, in-memory Central Limit Order Book matching engine designed for cryptocurrency spot markets. It accepts buy and sell orders, matches them according to strict price-time priority, maintains a full order book, generates trades, publishes market data, and records every event for auditability and recovery.

The system is built as a serious educational and portfolio project. It prioritizes correctness, clarity, determinism, and clean architecture over ultra-low-latency production optimizations (such as kernel bypass or FPGA). The design reflects the core architecture used by real cryptocurrency exchanges while remaining implementable by a single strong developer.

---

## 2. Goals and Non-Goals

### Goals
- Implement a correct and deterministic price-time priority matching engine.
- Support multiple order types and time-in-force policies.
- Maintain a full Level-2 order book per instrument.
- Provide real-time market data (BBO, depth, trades).
- Enforce basic pre-trade risk checks.
- Guarantee full recoverability through event journaling and snapshots.
- Apply clear design patterns (Strategy, State, Command, Event Sourcing, etc.).
- Serve as a strong, explainable project for technical interviews in fintech and trading systems.

### Non-Goals (Out of Scope for this version)
- Microsecond-level latency or high-frequency trading infrastructure.
- Perpetual futures, options, or margin trading (can be added later).
- Multi-region active-active deployment.
- Full KYC, fiat on-ramps, or custody.
- Advanced order types (Iceberg, Pegged, Hidden) in the initial version.
- Regulatory reporting or formal exchange licensing.

---

## 3. Functional Requirements

### Core Matching
- Support instruments (trading pairs) such as BTC-USDT, ETH-USDT, etc.
- Order types: Market, Limit, Stop, Stop-Limit.
- Time-in-Force: GTC (Good-Till-Cancel), IOC (Immediate-or-Cancel), FOK (Fill-or-Kill), Day.
- Strict price-time priority (better price first; at the same price, earlier order first).
- Partial fills with residual quantity management.
- Order cancellation and replacement (cancel/replace).
- Self-trade prevention (configurable: reject aggressor, cancel resting, or cancel both).

### Order Book & Market Data
- Full in-memory order book per instrument (bids and asks).
- Level-1 (BBO – Best Bid and Offer) and Level-2 (full depth) data.
- Real-time publication of trades, BBO updates, and depth updates.
- Snapshot + incremental update support for market data subscribers.

### Accounts & Risk
- Basic account model with available balance and positions.
- Pre-trade risk checks: sufficient balance, position limits, basic notional limits.
- Position and balance updates on fills.

### Reliability & Audit
- Append-only event journal of all commands and resulting events.
- Periodic snapshots of order book and account state.
- Full deterministic recovery by replaying the journal from a snapshot.
- Unique sequence numbers for all events.

### Interfaces
- REST API for order entry, cancellation, account queries, and snapshots.
- WebSocket API for real-time market data and private order/fill updates.

---

## 4. Non-Functional Requirements

- **Correctness & Determinism (Highest Priority):**  
  The same sequence of inputs must always produce the exact same trades and final order book state.

- **Performance Targets (Educational but Realistic):**
    - Matching path should complete in low milliseconds on commodity hardware.
    - Support thousands of orders per second per instrument on a single machine.

- **Single-Threaded Matching:**  
  One matching thread (or event loop) per instrument (or per shard of instruments) to guarantee ordering and eliminate lock contention on the book.

- **Durability:**  
  No acknowledged order or trade may be lost after a crash. Recovery must restore exact state.

- **Observability:**  
  Structured logging, basic metrics (orders/sec, trades/sec, book depth stats), and clear error reporting.

- **Simplicity of Deployment:**  
  Runnable as a single process or modest set of services for the portfolio version.

---

## 5. High-Level Architecture
