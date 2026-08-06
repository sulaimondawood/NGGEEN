# NGGEEN – Complete Implementation Phases (Crypto Matching Engine)

This version is designed for a **proper crypto exchange core** (not forex) with a real user dashboard.

---

## Phase 1: Core Matching Engine
**Goal:** Correct in-memory order book and matching.

- Order & OrderBook models
- Price-time priority matching
- Limit + Market orders
- GTC + IOC
- Place & Cancel order
- Console logging of book and trades

**Done when:** You can match orders correctly in memory.

---

## Phase 2: Determinism, Journal & Recovery
**Goal:** Crash safety and deterministic behavior.

- Sequence numbers
- Event Journal (append-only)
- Domain Events
- OrderBook Snapshots
- Full recovery (snapshot + replay)

**Done when:** Restarting the app restores the exact order book state.

---

## Phase 3: Multi-Market + Full Order Types
**Goal:** Support real crypto trading pairs and proper order types.

- Multiple instruments (BTC-USDT, ETH-USDT, etc.)
- Order types: Limit, Market, IOC, FOK, Stop, Stop-Limit
- Time-in-Force: GTC, IOC, FOK, Day
- Self-trade prevention
- Tick size & minimum quantity rules
- Instrument status (open/closed)

**Done when:** Multiple markets work correctly with the main order types.

---

## Phase 4: Accounts, Balances, Positions & Risk
**Goal:** Proper financial controls.

- User accounts
- Available balance + reserved balance
- Positions per instrument
- Pre-trade risk checks (sufficient balance, basic limits)
- Balance & position updates on trades
- Order history & trade history storage

**Done when:** Users cannot place orders they cannot afford, and balances update correctly.

---

## Phase 5: Backend APIs (REST + WebSocket)
**Goal:** Solid backend ready for a real frontend.

- Authentication (Register / Login + JWT or API Keys)
- REST API:
    - Place / Cancel / Get orders
    - Get order book (Level 2)
    - Get trades
    - Get account balance & positions
    - Get order & trade history
- WebSocket:
    - Public: order book, trades, ticker
    - Private: order updates, fills, balance changes
- Proper error responses and validation

**Done when:** The backend is fully usable via API and WebSocket.

---

## Phase 6: User Dashboard (Frontend)
**Goal:** A proper trading interface for users.

- Login / Register pages
- Trading page:
    - Order book (Level 2)
    - Recent trades
    - Order form (Limit / Market, Buy / Sell)
    - Open orders list (with cancel)
- Portfolio page:
    - Balances
    - Positions
    - Order history
    - Trade history
- Real-time updates via WebSocket
- Clean, professional UI (dark theme recommended)

**Done when:** A user can register, deposit (simulated), place trades, and see everything update live.

---

## Phase 7: Polish, Security & Portfolio Readiness
**Goal:** Make it solid and presentable.

- Input validation & better error handling
- Rate limiting
- Basic admin endpoints (optional but good)
- Comprehensive tests (especially matching + recovery)
- Structured logging
- README with architecture, design decisions, and how to run
- Deployment (optional but recommended)
- Demo script or short video

**Done when:** You can confidently show and explain the full system in an interview.

---