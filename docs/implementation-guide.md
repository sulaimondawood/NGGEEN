# NGGEEN – Complete Implementation Phases
**Crypto Matching Engine (CLOB)**

This guide is for building a **proper crypto exchange core** (spot), not forex.  
Goal: learn how real matching systems work, with a path to a presentable trading dashboard.

---

## Architecture Principles (All Phases)

- One **OrderBook per symbol**
- Live book stays **in memory**
- Matching is **deterministic**
- **Event Journal is the source of truth**
- DB is mainly for **query/history projections**
- API DTOs stay outside domain matching
- Prefer correctness first, concurrency later

---

## Phase 1: Core Matching Engine
**Goal:** Correct in-memory order book and matching.

### Scope
- `Order` and `OrderBook` models
- Price-time priority (FIFO within price)
- Order types: **Limit**, **Market**
- Basic place-order flow
- Match across multiple price levels
- Partial fills
- Rest residual limit orders on the book
- Market residual is **not** rested (cancel/expire remainder)
- Basic cancel-order flow
- Unit tests for matching scenarios

### Design notes
- Matching happens fully in memory
- No journal/recovery required yet
- No per-symbol worker queues required yet
- `Order` may be a JPA entity later, but matching must not depend on DB

### Done when
- Limit and market orders match correctly
- Residual limit orders rest correctly
- Market remainder is cancelled/expired
- Unit tests cover core matching cases

---

## Phase 2: Determinism, Journal & Recovery
**Goal:** Crash safety and deterministic rebuild of state.


### Scope
- Per-instrument SequenceGenerator (monotonic, scoped to each OrderBook — NOT global/shared)
- Assign `sequenceNo` at **order acceptance**
- Domain events:
  - `OrderAccepted`
  - `OrderRejected`
  - `TradeExecuted`
  - `OrderCancelled`
  - `OrderFilled` / `OrderPartiallyFilled`
- Append-only **Event Journal** (file or append-only table)
- Journal records sequenced commands/events
- **OrderBook Snapshots** (per symbol, with `lastSequenceNo`)
- Startup recovery:
  1. Load active instruments
  2. Create in-memory books
  3. Load latest snapshot
  4. Replay journal entries after snapshot sequence
  5. Resume sequence counter from max sequence
- Recovery tests (place orders → restart → book restored)
* Per-instrument single-threaded executor: sequence assignment, matching,
  and journal append all happen on the same dedicated thread per instrument
* No order is processed until it's confirmed durably journaled (or explicitly
  document if you're deferring durability-before-ack to a later phase)
### Design rules
- Journal is source of truth
- In-memory book is a projection
- Do **not** use `orderRepository.save()` as the recovery mechanism
- DB projections for orders/trades are optional in this phase
- Critical path: memory match + append journal  
  (avoid heavy synchronous relational writes inside fill loops)

### Done when
- Restarting the app restores the exact book state
- Replay is deterministic by sequence number
- Tests prove recovery works

---

## Phase 3: Multi-Market + Instrument Rules + Full Order Types
**Goal:** Real multi-market behavior and exchange-style order rules.

### Scope
- Multiple instruments (`BTC-USDT`, `ETH-USDT`, etc.)
- Instrument config in DB:
  - symbol, base/quote asset
  - tickSize, stepSize
  - minQuantity, minOrderValue
  - price/quantity precision
  - status (`TRADING`, `HALTED`)
- Startup loads active instruments into `OrderBookRegistry`
- Order types: Limit, Market, IOC, FOK, Stop, Stop-Limit
- Time-in-force: GTC, IOC, FOK, DAY
- Self-trade prevention (basic)
- Reject invalid prices/quantities using instrument rules
- Pre-acceptance instrument checks:
   - symbol exists
   - market is TRADING
   - price aligns to tickSize
   - quantity aligns to stepSize
   - minQuantity enforced
   - minOrderValue enforced

### Done when
- Multiple markets run independently
- Instrument rules are enforced
- Main order types and TIF behaviors are correct

---

## Phase 4: Accounts, Balances, Positions & Risk
**Goal:** Proper financial controls.

### Scope
- User/account model
- Available balance + reserved balance
- Pre-trade risk checks:
  - sufficient balance
  - basic order/position limits
- Reserve funds on accepted orders
- Release/consume funds on cancel/fill
- Position updates on trades
- Order history + trade history projections
- Event-driven balance updates from `TradeExecuted` / cancel events where possible

### Done when
- Users cannot place orders they cannot afford
- Balances and positions update correctly after trades/cancels

---

## Phase 5: Backend APIs (REST + WebSocket)
**Goal:** Production-like API surface for clients and dashboard.

### Scope
- Auth (Register/Login + JWT and/or API keys)
- REST:
  - Place / Cancel / Get order
  - Get order book (Level 2)
  - Get recent trades
  - Get balances/positions
  - Get order and trade history
- WebSocket:
  - Public: depth, trades, ticker
  - Private: order updates, fills, balance changes
- Request uses **symbol** (not internal instrument id)
- Support optional `clientOrderId` (idempotency per account)
- Consistent error responses

### Concurrency note
- Introduce per-symbol single-writer behavior here or late Phase 4:
  - start with per-book lock, or
  - symbol queue + single worker
- API threads accept requests; matching for one symbol stays serialized

### Done when
- Full trading flow works over REST + WebSocket clients

---

## Phase 6: User Dashboard (Frontend)
**Goal:** Usable trading interface.

### Scope
- Login / Register
- Trading view:
  - Level 2 order book
  - recent trades
  - order form (limit/market, buy/sell)
  - open orders + cancel
- Portfolio view:
  - balances
  - positions
  - order history
  - trade history
- Simulated deposit flow
- Real-time updates via WebSocket
- Clean UI (dark theme recommended)

### Done when
- A user can register, deposit (simulated), trade, and see live updates

---

## Phase 7: Polish, Security & Portfolio Readiness
**Goal:** Interview-ready system.

### Scope
- Strong validation and error handling
- Rate limiting
- Idempotency checks for `clientOrderId`
- Comprehensive tests:
  - matching
  - recovery/replay
  - risk
  - API contracts
- Structured logging and basic metrics
- README:
  - architecture
  - phase design decisions
  - how to run
  - recovery explanation
- Optional admin endpoints
- Optional deployment + demo video

### Done when
- You can explain and demo the system confidently

---

## Suggested Package Shape

```text
trade/
  api/                # controllers, request/response DTOs
  application/        # use-case services, registry, recovery orchestration
  model/              # Order, OrderBook, Instrument, Trade
  matching/           # matching strategies
  event/              # domain events
  journal/            # append-only journal
  snapshot/           # snapshot store
  infrastructure/     # JPA repos, file/db adapters
  mapper/             # request/entity mapping