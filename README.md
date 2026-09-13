# NGGEEN

**Crypto Matching Engine – A Complete Central Limit Order Book Implementation**

![Java](https://img.shields.io/badge/java-98.4%25-orange)
![HTML](https://img.shields.io/badge/html-1.6%25-red)
![License](https://img.shields.io/badge/license-MIT-green)

> A deterministic, in-memory Central Limit Order Book (CLOB) matching engine for cryptocurrency spot markets, built as an educational and portfolio project.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Core Components](#core-components)
- [Data Models](#data-models)
- [Matching Logic](#matching-logic)
- [API Reference](#api-reference)
- [Implementation Phases](#implementation-phases)
- [Design Decisions](#design-decisions)
- [Usage Examples](#usage-examples)
- [Testing & Determinism](#testing--determinism)
- [Future Extensions](#future-extensions)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

NGGEEN is a **deterministic, in-memory matching engine** designed for cryptocurrency spot markets. It accepts buy and sell orders, matches them according to strict **price-time priority**, maintains a full **order book**, generates trades, and publishes real-time market data.

### Why NGGEEN?

This project prioritizes **correctness, clarity, and determinism** over ultra-low-latency production optimizations. The design reflects real-world exchange architecture while remaining implementable and understandable by a single developer. It serves as both an **educational tool** and a **strong portfolio piece** for fintech and trading systems roles.

### Project Philosophy

- **Correctness First**: The same sequence of inputs must always produce identical trades and order book state.
- **Clean Architecture**: Separation of concerns using proven design patterns (Strategy, State, Command, Event Sourcing).
- **Full Auditability**: Every action is journaled for complete recovery and regulatory compliance.
- **Educational Value**: Code is optimized for understanding, not just performance.

---

## Key Features

### ✅ Core Matching Engine
- **Price-Time Priority Matching**: Best price first; at the same price, earlier orders first (FIFO)
- **Multiple Order Types**: Market, Limit, Stop, Stop-Limit
- **Time-in-Force Policies**: GTC (Good-Till-Cancel), IOC (Immediate-or-Cancel), FOK (Fill-or-Kill), Day
- **Partial Fills**: Support for residual quantity management
- **Self-Trade Prevention**: Configurable strategies (reject aggressor, cancel resting, or cancel both)

### 📊 Order Book & Market Data
- **Full In-Memory Order Book**: Per-instrument bids and asks
- **Level-1 Data**: Best Bid and Offer (BBO)
- **Level-2 Data**: Full order book depth
- **Real-Time Publications**: Trades and depth updates
- **Snapshot Support**: Efficient recovery and data sync

### 💰 Account Management
- **Balance Tracking**: Available and reserved balance
- **Position Management**: Per-instrument positions with average cost
- **Pre-Trade Risk Checks**: Sufficient balance, position limits, notional limits
- **Fund Reservation**: Automatic reservation on order placement

### 🔒 Reliability & Audit
- **Event Journaling**: Append-only log of all commands and events
- **Periodic Snapshots**: Fast recovery without full replay
- **Deterministic Recovery**: Replay journal from snapshot for exact state restoration
- **Unique Sequence Numbers**: Total ordering of all events

### 🌐 External Interfaces
- **REST API**: Order entry, cancellation, account queries, snapshots
- **WebSocket API** *(Coming in Phase 5)*: Real-time market data and order updates

---

## Architecture

### High-Level System Flow

```
Clients (UI / Bots / API Users)
         ↓
    ┌─────────────────────────┐
    │   API Gateway Layer     │  REST + WebSocket
    │   (Auth, Validation,    │  (Rate Limiting)
    │    Authentication)      │
    └────────────┬────────────┘
                 ↓
    ┌─────────────────────────┐
    │   Risk Engine           │  Pre-trade checks
    └────────────┬────────────┘
                 ↓
    ┌─────────────────────────┐
    │   Sequencer             │  Monotonic sequence numbers
    └────────────┬────────────┘
                 ↓
    ┌─────────────────────────┐
    │   Matching Engine       │  Single-threaded per instrument
    │   + Order Books         │  In-memory price-time books
    └────────────┬────────────┘
                 ↓
          ┌──────┴──────┐
          ↓             ↓
    ┌──────────────┐  ┌──────────────────┐
    │  Journal     │  │  Market Data     │
    │  + Snapshot  │  │  Publisher       │
    └──────────────┘  └────────┬─────────┘
                                ↓
                    Accounts / Positions Service
```

### Key Architectural Principles

1. **Deterministic State Machine**: The matching engine is a pure state machine driven by a totally ordered stream of commands.
2. **Event-Driven**: All side effects (persistence, market data, balance updates) happen after the match decision via emitted events.
3. **Single-Threaded Per Instrument**: One matching thread per instrument guarantees strict ordering and determinism.
4. **Separation of Concerns**: Matching logic, persistence, and market data publication are cleanly separated.
5. **Fail-Closed Risk**: If risk cannot be verified, the order is rejected.

---

## Getting Started

### Prerequisites

- **Java 11+** (Primary implementation language)
- **Maven 3.6+** or **Gradle 6.0+**
- **Git**
- (Optional) Docker for containerized deployment

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/sulaimondawood/NGGEEN.git
   cd NGGEEN
   ```

2. **Build the project**
   ```bash
   mvn clean install
   # OR
   gradle build
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   # OR
   gradle run
   ```

4. **Access the API**
   - REST API: `http://localhost:8080`

### Quick Start Example

```bash
# Place a buy order
curl -X POST http://localhost:8080/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "clientOrderId": "order-1",
    "accountId": "acc-123",
    "instrumentId": "BTC-USDT",
    "side": "BUY",
    "type": "LIMIT",
    "timeInForce": "GTC",
    "price": "45000.00",
    "quantity": "0.5"
  }'

# Get order book
curl http://localhost:8080/v1/instruments/BTC-USDT/book?depth=10
```

---

## Project Structure

```
NGGEEN/
├── src/
│   ├── main/
│   │   ├── java/com/nggeen/
│   │   │   ├── api/                    # REST controllers
│   │   │   │   ├── OrderController.java
│   │   │   │   ├── MarketDataController.java
│   │   │   │   └── AccountController.java
│   │   │   │
│   │   │   ├── matching/               # Core matching engine
│   │   │   │   ├── MatchingEngine.java
│   │   │   │   ├── OrderBook.java
│   │   │   │   ├── PriceLevel.java
│   │   │   │   └── MatchingStrategy.java
│   │   │   │
│   │   │   ├── model/                  # Domain models
│   │   │   │   ├── Order.java
│   │   │   │   ├── Trade.java
│   │   │   │   ├── Account.java
│   │   │   │   ├── Position.java
│   │   │   │   └── Instrument.java
│   │   │   │
│   │   │   ├── event/                  # Event sourcing
│   │   │   │   ├── Event.java
│   │   │   │   ├── OrderAcceptedEvent.java
│   │   │   │   ├── TradeExecutedEvent.java
│   │   │   │   ├── OrderCancelledEvent.java
│   │   │   │   └── EventJournal.java
│   │   │   │
│   │   │   ├── risk/                   # Risk management
│   │   │   │   ├── RiskEngine.java
│   │   │   │   ├── RiskCheck.java
│   │   │   │   └── BalanceValidator.java
│   │   │   │
│   │   │   ├── sequencer/              # Sequence generation
│   │   │   │   └── Sequencer.java
│   │   │   │
│   │   │   ├── persistence/            # Durability layer
│   │   │   │   ├── SnapshotStore.java
│   │   │   │   └── JournalStorage.java
│   │   │   │
│   │   │   ├── marketdata/             # Market data publishing
│   │   │   │   ├── MarketDataPublisher.java
│   │   │   │   ├── TradePublisher.java
│   │   │   │   └── DepthPublisher.java
│   │   │   │
│   │   │   └── config/                 # Configuration
│   │   │       └── ApplicationConfig.java
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       └── logback.xml
│   │
│   └── test/
│       ├── java/com/nggeen/
│       │   ├── matching/
│       │   │   ├── MatchingEngineTest.java
│       │   │   └── OrderBookTest.java
│       │   ├── event/
│       │   │   └── DeterminismTest.java
│       │   └── risk/
│       │       └── RiskEngineTest.java
│       │
│       └── resources/
│           └── test-data.json
│
├── docs/
│   ├── ARCHITECTURE.md
│   ├── API.md
│   ├── DESIGN_DECISIONS.md
│   └── RECOVERY.md
│
├── pom.xml                             # Maven configuration
├── build.gradle                        # Gradle configuration
├── README.md                           # This file
└── LICENSE
```

---

## Core Components

### 1. **Matching Engine**
The heart of the system. Executes price-time priority matching against the order book.

**Responsibilities:**
- Accept orders from the sequencer
- Execute matches according to price-time priority
- Emit trade events
- Manage residual order quantities
- Handle order cancellations and replacements

**Key Methods:**
```java
public List<Trade> placeOrder(Order order);
public boolean cancelOrder(String orderId);
public OrderBook getOrderBook(String instrumentId);
```

### 2. **Order Book**
In-memory representation of supply and demand for a single instrument.

**Data Structure:**
- **Bids**: TreeMap with descending prices → List of orders at each price (FIFO)
- **Asks**: TreeMap with ascending prices → List of orders at each price (FIFO)
- **OrderID Lookup**: HashMap for O(1) order retrieval

**Properties:**
- `lastTradePrice`: Most recent execution price
- `lastTradeQuantity`: Most recent execution size
- `tradingStatus`: Active/Halted/Closed

### 3. **Risk Engine**
Pre-trade validation to prevent invalid orders.

**Checks:**
- Sufficient available balance for buy orders
- Sufficient position for sell orders
- Position limits per instrument
- Notional exposure limits per account
- Daily trading limits (optional)

**Strategy:**
- **Fail-Closed**: If risk cannot be verified, reject the order.

### 4. **Sequencer**
Assigns monotonically increasing sequence numbers and timestamps.

**Purpose:**
- Provides total ordering of all commands
- Foundation for deterministic replay
- Enables recovery to any point in time

### 5. **Event Journal**
Append-only log of all commands and resulting events.

**Events Logged:**
- `OrderAcceptedEvent`: Order entered the matching engine
- `TradeExecutedEvent`: Orders matched
- `OrderCancelledEvent`: Order was cancelled
- `OrderRejectedEvent`: Order failed validation
- `AccountUpdatedEvent`: Balance/position changed

**Durability:**
- Written to persistent storage before acknowledgment
- Never loses acknowledged events
- Enables complete audit trail

### 6. **Snapshot Store**
Periodic snapshots of order books and accounts for fast recovery.

**Content:**
- Full state of all order books
- All account balances and positions
- Sequence number and timestamp
- Compression support for efficiency

**Purpose:**
- Fast recovery without full journal replay
- Reduces replay time significantly

### 7. **Market Data Publisher**
Consumes events and produces real-time market data.

**Outputs:**
- **Best Bid/Offer (BBO)**: Price and quantity of best buy/sell orders
- **Depth Updates**: Level-2 order book changes
- **Trades**: Executed trade details
- **Ticker**: 24h high/low, volume, change

**Subscribers:**
- ~~WebSocket clients~~ *(Coming in Phase 5)*
- External market data feeds
- Analytics systems

### 8. **Accounts Service**
Maintains user balances and positions.

**Operations:**
- Reserve funds when orders are placed
- Update balances and positions on trades
- Release reserved funds on order cancellation
- Enforce position limits

---

## Data Models

### Order
```java
public class Order {
    private String orderId;              // Unique order identifier
    private String clientOrderId;        // User-provided order ID
    private String accountId;            // Owner's account
    private String instrumentId;         // Trading pair (e.g., BTC-USDT)
    
    private OrderSide side;              // BUY or SELL
    private OrderType type;              // LIMIT, MARKET, STOP, STOP_LIMIT
    private TimeInForce timeInForce;     // GTC, IOC, FOK, DAY
    
    private BigDecimal price;            // Order price (null for market orders)
    private BigDecimal stopPrice;        // Stop trigger price (for stop orders)
    private BigDecimal quantity;         // Original quantity
    private BigDecimal remainingQuantity; // Unfilled quantity
    private BigDecimal executedQuantity;  // Filled quantity
    
    private OrderStatus status;          // PENDING, ACCEPTED, PARTIALLY_FILLED, FILLED, CANCELLED
    private BigDecimal averageFillPrice;  // Weighted average execution price
    
    private LocalDateTime createdAt;     // Order creation time
    private LocalDateTime updatedAt;     // Last status change
    private long sequenceNumber;         // Total order sequence
}
```

### Trade (Execution)
```java
public class Trade {
    private String tradeId;              // Unique trade identifier
    private String instrumentId;         // Trading pair
    
    private String buyOrderId;           // Buying order
    private String sellOrderId;          // Selling order
    private OrderSide aggressorSide;     // Which side initiated the trade
    
    private BigDecimal price;            // Execution price
    private BigDecimal quantity;         // Execution size
    private BigDecimal totalValue;       // price × quantity
    
    private LocalDateTime executedAt;    // Trade execution time
    private long sequenceNumber;         // Event sequence number
}
```

### OrderBook
```java
public class OrderBook {
    private String instrumentId;
    
    private TreeMap<BigDecimal, PriceLevel> bids;    // Descending price order
    private TreeMap<BigDecimal, PriceLevel> asks;    // Ascending price order
    private Map<String, Order> orderIndex;           // O(1) order lookup
    
    private BigDecimal lastTradePrice;
    private BigDecimal lastTradeQuantity;
    private LocalDateTime lastTradeTime;
    private TradingStatus tradingStatus;
}
```

### PriceLevel
```java
public class PriceLevel {
    private BigDecimal price;
    private BigDecimal totalQuantity;     // Aggregate depth at price
    private int orderCount;
    private Deque<Order> orders;          // FIFO queue for time priority
}
```

### Account
```java
public class Account {
    private String accountId;
    private String userId;
    
    private BigDecimal availableBalance;  // Can be used for new orders
    private BigDecimal reservedBalance;   // Tied up in existing orders
    private BigDecimal totalBalance;      // available + reserved
    
    private Map<String, Position> positions;  // Per-instrument positions
    private AccountStatus status;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### Position
```java
public class Position {
    private String instrumentId;
    private BigDecimal quantity;          // Current holding (positive = long)
    private BigDecimal averageCost;       // Weighted average entry price
    private BigDecimal unrealizedPnL;     // Current value - cost basis
}
```

### Event (Journal Entry)
```java
public class Event {
    private long sequenceNumber;          // Globally unique sequence
    private EventType type;               // ORDER_ACCEPTED, TRADE_EXECUTED, etc.
    private LocalDateTime timestamp;
    
    private String aggregateId;           // Order ID, Account ID, etc.
    private String aggregateType;         // ORDER, ACCOUNT, etc.
    
    private Map<String, Object> payload;  // Event-specific data
    private int version;                  // Aggregate version
}
```

---

## Matching Logic

### Price-Time Priority Algorithm

When a new order arrives (after sequencing and risk checks):

```
1. Validate order (size, price, account status)
2. IF order is IOC/FOK and is crossing the spread:
     STORE partial_fills = []
     FOR each price level on opposite side (best to worst):
       FOR each resting order at price (FIFO order):
         CALCULATE min(resting.remainingQty, aggressor.remainingQty)
         CREATE trade for matched quantity
         REDUCE both order quantities
         ADD trade to partial_fills
       IF aggressor.remainingQty == 0:
         BREAK (fully matched)
   ELSE IF order is GTC/DAY:
     (same logic as above for crossing)

3. Handle residual:
   IF order.remainingQty > 0:
     IF timeInForce == IOC:
       CANCEL residual (no resting)
     ELSE IF timeInForce == FOK:
       REJECT entire order (no partial fills allowed)
     ELSE:  // GTC or DAY
       INSERT order at its price level
       EMIT OrderAcceptedEvent

4. EMIT all TradeExecutedEvents
5. EMIT OrderBookUpdateEvent
6. UPDATE account balances and positions
7. RETURN list of trades
```

### Stop Order Handling

Stop orders are held in a separate **off-book queue** and triggered when:

- **Stop-Loss Order (Sell Stop)**: Last trade price ≤ stop price
- **Buy Stop**: Last trade price ≥ stop price

**Triggering Process:**
1. Stop condition is detected after a trade
2. Stop order is converted to market or limit order
3. Order enters normal matching logic
4. Stop order is removed from off-book queue

### Self-Trade Prevention Strategies

Three configurable approaches when the same account would trade with itself:

1. **Reject Aggressor**: Cancel the incoming order if it would self-trade
2. **Cancel Resting**: Cancel the resting order if the aggressor would self-trade
3. **Cancel Both**: Cancel both orders if they would self-trade (most conservative)

---

## API Reference

### REST Endpoints

#### Orders

**Place Order**
```http
POST /v1/orders
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>

{
  "clientOrderId": "client-ord-1",
  "accountId": "acc-123",
  "instrumentId": "BTC-USDT",
  "side": "BUY",
  "type": "LIMIT",
  "timeInForce": "GTC",
  "price": "45000.00",
  "quantity": "0.5"
}

Response 201:
{
  "orderId": "ord-abc123",
  "clientOrderId": "client-ord-1",
  "status": "ACCEPTED",
  "sequenceNumber": 1000001,
  "createdAt": "2025-01-15T10:30:45Z"
}
```

**Get Order**
```http
GET /v1/orders/{orderId}
Authorization: Bearer <JWT_TOKEN>

Response 200:
{
  "orderId": "ord-abc123",
  "clientOrderId": "client-ord-1",
  "instrumentId": "BTC-USDT",
  "side": "BUY",
  "type": "LIMIT",
  "price": "45000.00",
  "quantity": "0.5",
  "executedQuantity": "0.25",
  "remainingQuantity": "0.25",
  "averageFillPrice": "45100.00",
  "status": "PARTIALLY_FILLED",
  "createdAt": "2025-01-15T10:30:45Z",
  "updatedAt": "2025-01-15T10:31:12Z"
}
```

**Cancel Order**
```http
DELETE /v1/orders/{orderId}
Authorization: Bearer <JWT_TOKEN>

Response 200:
{
  "orderId": "ord-abc123",
  "status": "CANCELLED",
  "cancelledAt": "2025-01-15T10:32:00Z",
  "cancelledQuantity": "0.25"
}
```

#### Order Book

**Get Order Book (Level-2)**
```http
GET /v1/instruments/{instrumentId}/book?depth=20
Accept: application/json

Response 200:
{
  "instrumentId": "BTC-USDT",
  "sequenceNumber": 1000150,
  "timestamp": "2025-01-15T10:35:22Z",
  "bids": [
    {"price": "45100.00", "quantity": "2.5", "orders": 3},
    {"price": "45099.50", "quantity": "1.2", "orders": 2},
    ...
  ],
  "asks": [
    {"price": "45100.50", "quantity": "1.8", "orders": 2},
    {"price": "45101.00", "quantity": "3.0", "orders": 4},
    ...
  ],
  "lastTradePrice": "45100.00",
  "lastTradeQuantity": "0.25",
  "lastTradeTime": "2025-01-15T10:35:20Z"
}
```

#### Accounts

**Get Account**
```http
GET /v1/accounts/{accountId}
Authorization: Bearer <JWT_TOKEN>

Response 200:
{
  "accountId": "acc-123",
  "userId": "user-456",
  "availableBalance": "50000.00",
  "reservedBalance": "22500.00",
  "totalBalance": "72500.00",
  "positions": [
    {
      "instrumentId": "BTC-USDT",
      "quantity": "0.5",
      "averageCost": "44800.00",
      "currentPrice": "45100.00",
      "unrealizedPnL": "150.00"
    }
  ],
  "status": "ACTIVE"
}
```

---

## Implementation Phases

### Phase 1: Core Matching Engine (Foundation)
**Goal:** Build a correct in-memory order book that can match orders.

**Deliverables:**
- ✅ Basic Order model
- ✅ OrderBook implementation (bids/asks)
- ✅ Price-time priority matching algorithm
- ✅ Support: Limit and Market orders
- ✅ Support: GTC and IOC time-in-force
- ✅ Order placement and cancellation
- ✅ Console output of trades and order book

**Key Tests:**
- Limit order matching
- Market order matching
- Partial fills
- Order cancellation

---

### Phase 2: Determinism & Recovery
**Goal:** Make the engine crash-safe and fully deterministic.

**Deliverables:**
- ✅ Sequence number generation
- ✅ Event journal (append-only log)
- ✅ Domain events (OrderAccepted, TradeExecuted, OrderCancelled)
- ✅ OrderBook snapshots
- ✅ Recovery process (snapshot + replay journal)

**Key Tests:**
- Replay from snapshot
- Determinism verification (same input → same output)
- Recovery after simulated crash

---

### Phase 3: Multiple Instruments & Expanded Order Types
**Goal:** Make the system feel like a real multi-market exchange.

**Deliverables:**
- ✅ Multiple trading pairs (BTC-USDT, ETH-USDT, etc.)
- ✅ FOK (Fill-or-Kill) orders
- ✅ Stop and Stop-Limit orders
- ✅ Day time-in-force
- ✅ Self-trade prevention
- ✅ Instrument configuration (tick size, min quantity)

**Key Tests:**
- Cross-instrument order placement
- Stop order triggering
- Self-trade prevention scenarios

---

### Phase 4: Accounts, Balances & Pre-Trade Risk
**Goal:** Add the financial control layer.

**Deliverables:**
- ✅ Account model (balances + positions)
- ✅ Fund reservation on order placement
- ✅ Balance and position updates on trades
- ✅ Pre-trade risk checks:
  - Sufficient balance
  - Position limits
  - Notional limits
- ✅ Order rejection on failed risk checks

**Key Tests:**
- Insufficient balance rejection
- Position limit enforcement
- Balance updates after trades

---

### Phase 5: APIs (REST + WebSocket)
**Goal:** Expose the engine to external clients.

**Current Status:** ⏳ **In Progress**

**Deliverables:**
- ✅ REST API (place, cancel, get orders, order book, account)
- ⏳ WebSocket API *(Coming soon)*
  - Public market data (BBO, depth, trades)
  - Private order updates and fills
- ✅ JWT authentication
- ✅ API key authentication
- ⏳ Rate limiting *(Planned)*

**Key Tests:**
- API endpoint integration tests
- WebSocket message ordering *(when implemented)*
- Authentication and authorization

---

### Phase 6: Polish & Portfolio Ready
**Goal:** Make the project clean, testable, and presentable.

**Deliverables:**
- ⏳ Clean DDD project structure *(In progress)*
- ⏳ Comprehensive error handling *(In progress)*
- ⏳ Structured logging *(In progress)*
- ⏳ Metrics and monitoring *(Planned)*
- ⏳ Unit tests (especially matching logic) *(In progress)*
- ⏳ Integration tests *(In progress)*
- ⏳ Determinism / replay tests *(Planned)*
- ✅ Architecture documentation
- ✅ API documentation
- ⏳ Demo script or simple UI *(Planned)*

---

## Design Decisions

| Decision | Choice | Rationale | Trade-off |
|----------|--------|-----------|-----------|
| **Matching Concurrency** | Single-threaded per instrument | Guarantees strict ordering and determinism; eliminates lock contention | Cannot scale a single instrument across CPU cores |
| **Order Book Storage** | Fully in-memory | Fast access and simple reasoning | Requires journaling + snapshots for durability |
| **Matching Rule** | Price-Time (FIFO) | Standard for spot crypto; simple and fair | Does not support pro-rata |
| **Persistence** | Event journal + snapshots | Enables exact replay; regulatory audit trail | Slightly more complex than direct DB updates |
| **Risk Checks** | Pre-trade, fail-closed | Prevents invalid orders from entering the book | Adds latency before matching |
| **Market Data** | Event-driven publish | Keeps matching core clean and decoupled | Requires careful sequencing of messages |
| **Recovery Speed** | Snapshots + replay | Fast startup without hours of journal replay | Requires periodic snapshot creation |
| **Instrument Scaling** | Sharding by symbol | Allows horizontal scaling by assigning symbols to different instances | Requires external coordination layer |

---

## Usage Examples

### Example 1: Simple Buy/Sell Match

```bash
# 1. Place a sell order (resting)
curl -X POST http://localhost:8080/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "clientOrderId": "sell-1",
    "accountId": "acc-123",
    "instrumentId": "BTC-USDT",
    "side": "SELL",
    "type": "LIMIT",
    "timeInForce": "GTC",
    "price": "45100.00",
    "quantity": "1.0"
  }'

# Order is now resting on the order book at $45,100

# 2. Place an aggressive buy order
curl -X POST http://localhost:8080/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "clientOrderId": "buy-1",
    "accountId": "acc-456",
    "instrumentId": "BTC-USDT",
    "side": "BUY",
    "type": "LIMIT",
    "timeInForce": "GTC",
    "price": "45100.00",
    "quantity": "1.0"
  }'

# TRADE EXECUTED: 1.0 BTC at $45,100
# - Seller receives 45,100 USDT
# - Buyer receives 1.0 BTC
# Both orders are fully filled
```

### Example 2: Market Order with Partial Fill

```bash
# Order book state:
# Asks: 0.3 @ $45,100.00
#       0.7 @ $45,100.50

curl -X POST http://localhost:8080/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "clientOrderId": "market-buy-1",
    "accountId": "acc-123",
    "instrumentId": "BTC-USDT",
    "side": "BUY",
    "type": "MARKET",
    "timeInForce": "IOC",
    "quantity": "0.5"
  }'

# TRADES EXECUTED:
# Trade 1: 0.3 BTC @ $45,100.00
# Trade 2: 0.2 BTC @ $45,100.50 (from the 0.7 order)
# Total filled: 0.5 BTC
# Average price: $45,100.10

# Order status: FILLED
# Remaining: 0.0 BTC
```

### Example 3: Stop Order Trigger

```bash
# 1. Place a sell stop order (triggers if price drops to $44,500)
curl -X POST http://localhost:8080/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "clientOrderId": "stop-sell-1",
    "accountId": "acc-123",
    "instrumentId": "BTC-USDT",
    "side": "SELL",
    "type": "STOP",
    "stopPrice": "44500.00",
    "quantity": "1.0"
  }'

# Order is now off-book, waiting for price to reach stop

# 2. Market sells down to $44,500 (or below)
# → Stop order is automatically converted to market sell
# → Order enters the matching engine
# → Order is filled at best available price

# Order status update:
# { "orderId": "...", "status": "TRIGGERED" }
# { "orderId": "...", "status": "FILLED", "price": "44450.00" }
```

### Example 4: Deterministic Replay

```bash
# 1. Load latest snapshot
curl http://localhost:8080/v1/snapshot/latest

# Returns snapshot with sequence number 500,000

# 2. Replay journal from sequence 500,001 onwards
curl -X POST http://localhost:8080/v1/recovery/replay \
  -H "Content-Type: application/json" \
  -d '{ "fromSequence": 500001 }'

# System replays all events and restores exact state
# Matches should be identical to original run

# Determinism verification test passes ✓
```

---

## Testing & Determinism

### Test Categories

#### 1. Unit Tests (Matching Logic)
```java
@Test
public void testLimitOrderMatching() {
    // Setup order book with resting sell order
    Order restingSell = new Order(..., "SELL", "LIMIT", "45100.00", "1.0");
    orderBook.placeOrder(restingSell);
    
    // Place aggressive buy order
    Order aggressiveBuy = new Order(..., "BUY", "LIMIT", "45100.00", "1.0");
    List<Trade> trades = matchingEngine.placeOrder(aggressiveBuy);
    
    // Assert
    assertEquals(1, trades.size());
    assertEquals("45100.00", trades.get(0).getPrice());
    assertEquals("1.0", trades.get(0).getQuantity());
    assertTrue(orderBook.getOrderBook(instrument).isEmpty());
}

@Test
public void testPriceTimePriority() {
    // Setup: three sell orders at same price, different times
    Order sell1 = placeOrder(..., "45100.00", "0.3");
    Order sell2 = placeOrder(..., "45100.00", "0.5");
    Order sell3 = placeOrder(..., "45100.00", "0.2");
    
    // Buy 0.8 BTC
    Order buy = placeOrder(..., "BUY", "45100.00", "0.8");
    
    // Assert: matched with sell1, then sell2 (in time order)
    assertEquals("sell1", trades.get(0).getSellOrderId());
    assertEquals("sell2", trades.get(1).getSellOrderId());
    assertEquals("0.3", trades.get(0).getQuantity());
    assertEquals("0.5", trades.get(1).getQuantity());
}
```

#### 2. Determinism Tests
```java
@Test
public void testDeterministicReplay() throws Exception {
    // Record sequence of commands
    List<OrderCommand> commands = loadTestScenario();
    
    // Execute once
    MatchingEngine engine1 = new MatchingEngine();
    List<Trade> trades1 = executeCommands(engine1, commands);
    OrderBook book1 = engine1.getOrderBook(instrument);
    
    // Execute again (with snapshot + replay)
    MatchingEngine engine2 = new MatchingEngine();
    engine2.loadFromSnapshot(snapshot);
    engine2.replayJournal(commands);
    List<Trade> trades2 = executeCommands(engine2, commands);
    OrderBook book2 = engine2.getOrderBook(instrument);
    
    // Assert: identical results
    assertEquals(trades1, trades2);
    assertEquals(book1, book2);
}
```

#### 3. Risk Engine Tests
```java
@Test
public void testInsufficientBalanceRejection() {
    Account account = new Account("acc-123", "100.00" /* balance */);
    
    // Try to buy 1.0 BTC at $45,100 (requires 45,100 USDT)
    Order order = new Order(..., "BUY", "LIMIT", "45100.00", "1.0");
    
    RiskCheckResult result = riskEngine.validate(account, order);
    
    assertFalse(result.isValid());
    assertEquals("INSUFFICIENT_BALANCE", result.getReasonCode());
}
```

#### 4. Integration Tests
```java
@Test
public void testEndToEndOrderFlow() {
    // Setup: multiple accounts, instruments, orders
    // Execute: place orders, generate trades, check updates
    // Assert: accounts, positions, market data all consistent
}
```

### Determinism Guarantees

The system provides the following determinism guarantees:

✅ **Same Inputs → Same Outputs**  
- Given the same sequence of commands (orders, cancellations) in the same order, the matching engine will always produce identical trades and order book state.

✅ **Replay Fidelity**  
- Loading a snapshot and replaying all subsequent journal events recreates the exact system state without divergence.

✅ **Event Ordering**  
- The sequencer ensures a total order of all commands, preventing non-deterministic thread scheduling issues.

✅ **Round-Trip Consistency**  
- Snapshots can be taken at any point and used to recover to that exact state, potentially months or years later.

---

## Future Extensions (Post-MVP)

### High Priority
- [ ] **WebSocket API** (Phase 5)
  - Real-time market data streaming
  - Private order and fill updates
  - Connection management and reconnection logic

- [ ] **Perpetual Futures Support**
  - Mark price, funding rates, liquidations
  - Position sizing and leverage
  - Bankruptcy handling

- [ ] **Fee Model & Settlement**
  - Configurable fee structures (maker/taker)
  - Commission deduction from fills
  - Fee settlement to providers

- [ ] **Advanced Order Types**
  - Iceberg orders (hidden quantity)
  - Pegged orders (price relative to BBO)
  - Trailing stop orders

### Medium Priority
- [ ] **FIX Protocol Gateway**
  - Full FIX 4.4 / 5.0 support
  - Institutional client connectivity

- [ ] **Multi-Region Replication**
  - Primary-standby failover
  - Cross-region synchronization
  - Disaster recovery

- [ ] **Advanced Risk Management**
  - Portfolio margin calculations
  - VaR (Value-at-Risk) limits
  - Scenario analysis

### Lower Priority
- [ ] **Options Trading**
  - Black-Scholes pricing
  - Greeks calculations
  - Exercise and assignment handling

- [ ] **Margin & Lending**
  - Interest rate models
  - Collateral requirements
  - Liquidation auctions

- [ ] **Market Surveillance**
  - Suspicious activity detection
  - Pattern matching algorithms
  - Regulatory reporting

---

## Contributing

We welcome contributions! Please follow these guidelines:

1. **Fork the repository**
   ```bash
   git clone https://github.com/yourusername/NGGEEN.git
   ```

2. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Write tests** for your changes
   - Ensure determinism tests pass
   - Add unit and integration tests
   - Validate edge cases

4. **Follow code style**
   - Use Google Java Style Guide
   - Format with `mvn spotless:apply`
   - Document public APIs

5. **Commit with clear messages**
   ```bash
   git commit -m "feat: add iceberg order support"
   ```

6. **Submit a pull request**
   - Reference related issues
   - Describe the change and why it's needed
   - Include test results

---

## Architecture & Design Documentation

For deeper understanding, refer to:

- **[ARCHITECTURE.md](docs/ARCHITECTURE.md)** – Detailed component descriptions and interactions
- **[API.md](docs/API.md)** – Complete API reference with examples
- **[DESIGN_DECISIONS.md](docs/DESIGN_DECISIONS.md)** – Rationale for key choices
- **[RECOVERY.md](docs/RECOVERY.md)** – Recovery and disaster procedures
- **[PERFORMANCE.md](docs/PERFORMANCE.md)** – Performance characteristics and optimization tips

---

## Troubleshooting

### Order not matching when expected

**Possible causes:**
- Insufficient balance (check account balance)
- Order price doesn't cross the spread (check order book)
- Order time-in-force is IOC and no immediate match (convert to GTC)
- Trading is halted for the instrument (check trading status)

**Solution:**
```bash
# Check account balance
curl http://localhost:8080/v1/accounts/{accountId} -H "Authorization: Bearer $TOKEN"

# Check order book
curl http://localhost:8080/v1/instruments/BTC-USDT/book

# Check order status
curl http://localhost:8080/v1/orders/{orderId} -H "Authorization: Bearer $TOKEN"
```

### Determinism test failure

**Possible causes:**
- Non-deterministic random number generation
- Floating-point arithmetic with insufficient precision
- Concurrent modifications to shared state
- Journal replay did not capture all events

**Solution:**
- Use BigDecimal for all monetary values
- Ensure single-threaded matching per instrument
- Verify journal is complete and in sequence order
- Run determinism test with detailed logging enabled

---

## License

This project is licensed under the **MIT License** – see the [LICENSE](LICENSE) file for details.

---

## Support & Questions

- **Issues & Bugs**: [GitHub Issues](https://github.com/sulaimondawood/NGGEEN/issues)
- **Discussions**: [GitHub Discussions](https://github.com/sulaimondawood/NGGEEN/discussions)
- **Email**: [contact information]

---

## Acknowledgments

This project was inspired by real-world exchange architectures and designed with input from:
- Exchange engineering best practices
- Event sourcing patterns (CQRS)
- Clean architecture principles
- Community feedback and testing

Special thanks to all contributors and testers who have helped improve this project.

---

## Roadmap

**2025 Q1**
- [x] Phase 1 completion (Core matching engine)
- [x] Phase 2 completion (Event journal & recovery)
- [x] Basic documentation
- [ ] Phase 3 completion (Multiple instruments & advanced order types)
- [ ] Phase 4 completion (Accounts & risk management)

**2025 Q2**
- [ ] Phase 5 completion (REST API ✅ + WebSocket API ⏳)
- [ ] Performance benchmarking
- [ ] Additional test coverage

**2025 Q3**
- [ ] Phase 6 completion (Polish & testing)
- [ ] Portfolio version release
- [ ] Comprehensive documentation

**2025 Q4 & Beyond**
- [ ] Perpetual futures support
- [ ] Advanced order types (Iceberg, Pegged)
- [ ] FIX protocol gateway
- [ ] Multi-region replication

---

## Disclaimer

⚠️ **This is an educational project.** While NGGEEN implements sound matching engine principles, it is not intended for production use with real funds. Use at your own risk. Always thoroughly test and audit any trading system before using with real capital.

---

**Built with ❤️ for learning and fintech excellence.**

Last Updated: January 15, 2025  
Version: 1.0.0
