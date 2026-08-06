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
Clients (UI / Bots / API Users)
│
▼
┌─────────────────────────┐
│   API Gateway Layer     │  REST + WebSocket
│   (Auth, Validation,    │
│    Rate Limiting)       │
└────────────┬────────────┘
│
▼
┌─────────────────────────┐
│   Risk Engine           │  Pre-trade checks
└────────────┬────────────┘
│
▼
┌─────────────────────────┐
│   Sequencer             │  Assigns monotonic sequence number
└────────────┬────────────┘
│
▼
┌─────────────────────────┐
│   Matching Engine       │  Single-threaded per instrument
│   + Order Books         │  In-memory price-time books
└────────────┬────────────┘
│
┌─────┴─────┐
▼           ▼
┌────────────┐  ┌──────────────────┐
│  Journal   │  │ Market Data      │
│  + Snapshot│  │ Publisher        │
└────────────┘  └────────┬─────────┘
│
▼
Accounts / Positions Service


**Key Principles**
- The Matching Engine is a pure, deterministic state machine driven by a totally ordered stream of commands.
- All side effects (persistence, market data, balance updates) happen *after* the match decision via emitted events.
- Instruments can be sharded later by assigning different symbols to different matching instances.

---

## 6. Core Components

1. **API Gateway**  
   Handles REST and WebSocket connections, authentication (JWT + API keys), basic validation, and rate limiting. Translates external requests into internal Commands.

2. **Risk Engine**  
   Performs pre-trade checks against account balances, position limits, and instrument status. Fail-closed: if risk cannot be verified, the order is rejected.

3. **Sequencer**  
   Assigns a globally increasing sequence number and timestamp to every accepted command. This is the foundation of determinism.

4. **Matching Engine + Order Book**
    - Maintains one order book per instrument.
    - Data structures: sorted price levels + FIFO queue at each price + OrderID lookup map.
    - Executes price-time matching.
    - Emits events for every state change.

5. **Event Journal & Snapshot Store**  
   Append-only log of Commands and Events. Periodic full snapshots of books and accounts for fast recovery.

6. **Market Data Publisher**  
   Consumes events and produces BBO, depth, and trade messages for WebSocket subscribers. Supports snapshot requests.

7. **Accounts & Positions Service**  
   Maintains balances and positions. Applies fills and updates available buying power.

---

## 7. Key Data Models

### Order
- `orderId`, `clientOrderId`, `accountId`, `instrumentId`
- `side` (Buy/Sell), `type`, `timeInForce`
- `price`, `stopPrice`, `quantity`, `remainingQuantity`
- `status`, `createdAt`, `sequenceNumber`

### PriceLevel
- `price`
- `totalQuantity` (depth)
- `orderCount`
- FIFO list of orders

### OrderBook
- `instrumentId`
- `bids` (descending price)
- `asks` (ascending price)
- `lastTradePrice`, `lastTradeQuantity`
- `tradingStatus`

### Trade (Execution)
- `tradeId`, `instrumentId`, `price`, `quantity`
- `buyOrderId`, `sellOrderId`, `aggressorSide`
- `sequenceNumber`, `timestamp`

### Event (Journal entry)
- `sequenceNumber`, `type`, `payload`, `timestamp`

### Account
- `accountId`, `availableBalance`, `reservedBalance`
- `positions` (instrument → quantity + average price)

---

## 8. Matching Logic (Price-Time Priority)

When a new order arrives (after sequencing and risk checks):

1. If it is a Market order or a crossing Limit order, walk the opposite side of the book starting at the best price.
2. At each price level, match against resting orders in FIFO (time priority) order.
3. Generate a Trade event for every match. Reduce quantities on both sides.
4. Remove fully filled resting orders. Keep residual quantity on the aggressor if any remains.
5. If residual remains and Time-in-Force allows resting (GTC/Day), insert the remaining quantity into the book at its price level.
6. If IOC → cancel any residual. If FOK and cannot fill completely → reject the entire order (no partial fills).
7. Emit all resulting events (trades, book updates, order status changes).

Stop orders are held off-book and triggered when the last trade price or BBO reaches the stop price.

---

## 9. Critical Design Decisions & Trade-offs

| Decision              | Choice                          | Reason                                                                 | Trade-off                                      |
|-----------------------|---------------------------------|------------------------------------------------------------------------|------------------------------------------------|
| Matching concurrency  | Single-threaded per instrument  | Guarantees strict ordering and determinism; eliminates lock contention | Cannot scale a single instrument across cores  |
| Order book storage    | Fully in-memory                 | Fast access and simple reasoning                                       | Requires journaling + snapshots for durability |
| Matching rule         | Pure Price-Time (FIFO)          | Standard for spot crypto and equities; simple and fair                 | Does not support pro-rata                      |
| Persistence           | Event journal + snapshots       | Enables exact replay and regulatory-style audit                        | Slightly more complex than direct DB updates   |
| Risk checks           | Pre-trade, fail-closed          | Prevents invalid orders from entering the book                         | Adds latency before matching                   |
| Market data           | Event-driven publish            | Keeps matching core clean and decoupled                                | Requires careful sequencing of messages        |

---

## 10. Key Flows

### New Limit Order (Happy Path)
1. Client sends order via REST.
2. API Gateway authenticates and validates format.
3. Risk Engine checks balance and limits.
4. Sequencer assigns sequence number.
5. Matching Engine attempts to match against the opposite side.
6. Trades (if any) are generated and remaining quantity is rested.
7. Events are written to the journal and published.
8. Client receives acknowledgment + fill information.
9. Market data subscribers receive trade and book updates.
10. Accounts service updates balances and positions.

### Recovery
1. Load the latest snapshot.
2. Replay all journal events after the snapshot sequence number.
3. Restore exact order books, accounts, and sequence state.

---

## 11. API Sketch (High Level)

### REST
- `POST /v1/orders` – Place order
- `DELETE /v1/orders/{orderId}` – Cancel
- `GET /v1/orders/{orderId}`
- `GET /v1/accounts/{accountId}`
- `GET /v1/instruments/{symbol}/book?depth=10`
- `GET /v1/instruments/{symbol}/trades`

### WebSocket
- Public: `book`, `trades`, `ticker` channels
- Private: order updates and fills (authenticated)

All mutating operations require authentication (JWT or API key).

---

## 12. Technology Recommendations (Flexible)

- **Language:** Go or Rust (strong for concurrent, correct systems) or Java/Kotlin if preferred.
- **In-memory structures:** Custom order book or well-tested libraries.
- **Journal:** Append-only file or database log.
- **API:** Native HTTP + WebSocket support of the chosen language.
- **Persistence:** PostgreSQL for accounts and secondary indexes; Redis optional for session/cache.

The design itself is language-agnostic. Correctness and clarity matter more than the specific stack.

---

## 13. Implementation Phases

## Phase 1: Core Matching Engine (Foundation)

**Goal:** Build a correct in-memory order book that can match orders.

### Scope
- `Order` basic model
- `OrderBook` (bids and asks)
- Price-time priority matching
- Supported order types:
    - Limit
    - Market
- Time-in-Force:
    - GTC
    - IOC
- Operations:
    - Place order
    - Cancel order
- Console output of trades and order book state

### Learning Focus
- Price levels and depth
- Aggressor vs resting orders
- Partial fills
- Basic order book data structures

### Done When
You can place buy/sell orders, see them rest or match, and cancel them. Everything still lives only in memory.

---

## Phase 2: Determinism & Recovery

**Goal:** Make the engine crash-safe and fully deterministic.

### Scope
- Sequence numbers for every command
- Event Journal (append-only log)
- Domain Events (`OrderAccepted`, `TradeExecuted`, `OrderCancelled`, etc.)
- OrderBook snapshots
- Recovery process (load snapshot + replay journal)

### Learning Focus
- Event Sourcing
- Why matching engines must be deterministic
- How real exchanges recover after failures

### Done When
You can restart the application and the order book returns to the exact same state.

---

## Phase 3: Multiple Instruments + Expanded Order Types

**Goal:** Make the system feel like a real multi-market exchange.

### Scope
- Support multiple trading pairs (e.g. BTC-USDT, ETH-USDT)
- Additional order types:
    - FOK (Fill-or-Kill)
    - Stop
    - Stop-Limit (basic)
- Time-in-Force: Day
- Self-trade prevention (simple version)
- Basic instrument rules (tick size, minimum quantity)

### Done When
Multiple markets can run at the same time and the main order types behave correctly.

---

## Phase 4: Accounts, Balances & Pre-Trade Risk

**Goal:** Add the financial control layer.

### Scope
- Account model (available balance + positions)
- Fund reservation when placing orders
- Balance and position updates on trades
- Basic pre-trade risk checks:
    - Sufficient balance
    - Simple position limits
- Reject orders that fail risk checks

### Learning Focus
- Available vs reserved balance
- How exchanges prevent users from over-spending

### Done When
Orders are only accepted when the account can afford them, and balances update correctly after trades.

---

## Phase 5: APIs (REST + WebSocket)

**Goal:** Expose the engine to external clients.

### Scope
- REST API:
    - Place order
    - Cancel order
    - Get order
    - Get order book (Level 2)
    - Get account balance and positions
- WebSocket:
    - Public market data (BBO, depth, trades)
    - Private order updates and fills
- Basic authentication (JWT or API keys)

### Done When
You can place orders and watch the order book update in real time from an external client.

---

## Phase 6: Polish & Portfolio Ready

**Goal:** Make the project clean, testable, and presentable.

### Scope
- Clean project structure (DDD-style)
- Proper error handling
- Structured logging
- Basic metrics
- Unit tests (especially matching logic)
- Determinism / replay tests
- README with architecture explanation
- Optional simple UI or demo script

### Done When
You can confidently explain, demo, and show the code in a technical interview.

## 14. Future Extensions (Post-MVP)

- Perpetual futures (mark price, funding, liquidations)
- Additional matching strategies (Strategy pattern)
- FIX gateway
- Primary/standby replication
- Fee model and detailed trade reporting
- More sophisticated risk (portfolio margin)

---

## 15. Summary

NGGEEN is designed as a correct, deterministic, and understandable crypto matching engine. It focuses on the real core of exchange systems: the order book, price-time matching, event sourcing, and clean separation of concerns.

The architecture deliberately avoids unnecessary distributed-systems complexity so that the important financial and systems concepts remain clear. This makes the project both an excellent learning vehicle and a strong, explainable portfolio piece.