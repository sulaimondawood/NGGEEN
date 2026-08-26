package com.dawood.nggeen.trade.application;

import com.dawood.nggeen.account.application.AccountBalanceService;
import com.dawood.nggeen.account.infrastructure.persistence.AccountRepository;
import com.dawood.nggeen.account.infrastructure.persistence.UserRepository;
import com.dawood.nggeen.account.model.Account;
import com.dawood.nggeen.account.model.User;
import com.dawood.nggeen.account.model.enums.AccountStatus;
import com.dawood.nggeen.account.model.enums.AccountType;
import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.shared.exception.InvalidOrderException;
import com.dawood.nggeen.shared.exception.NggeenException;
import com.dawood.nggeen.shared.exception.ResourceNotFoundException;
import com.dawood.nggeen.trade.api.rest.dto.OrderResponse;
import com.dawood.nggeen.trade.api.rest.dto.PlaceOrderRequest;
import com.dawood.nggeen.trade.event.DomainEvent;
import com.dawood.nggeen.trade.infrastructure.journal.chronicle.ChronicleQueueService;
import com.dawood.nggeen.trade.mapper.OrderMapper;
import com.dawood.nggeen.trade.model.Instrument;
import com.dawood.nggeen.trade.model.Order;
import com.dawood.nggeen.trade.model.OrderBook;
import com.dawood.nggeen.trade.model.OrderBookSnapshot;
import com.dawood.nggeen.trade.model.enums.EventType;
import com.dawood.nggeen.trade.model.enums.OrderSide;
import com.dawood.nggeen.trade.model.enums.OrderType;
import com.dawood.nggeen.trade.service.FileSnapShotStore;
import com.dawood.nggeen.trade.service.InstrumentValidator;
import com.dawood.nggeen.trade.service.OrderBookRegistry;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeApplicationService {
    private final OrderBookRegistry orderBookRegistry;
    private final ChronicleQueueService chronicleQueueService;
    private final FileSnapShotStore fileSnapShotStore;
    private final InstrumentValidator instrumentValidator;
    private final UserRepository userRepository;
    private final AccountBalanceService accountBalanceService;
    private final AccountRepository accountRepository;

    private static final long SNAPSHOT_INTERVAL = 50_000L;

    public void processIncomingOrder(PlaceOrderRequest orderRequest) {
        if (orderRequest == null) {
            throw new InvalidOrderException(
                    ErrorCode.INVALID_REQUEST,
                    "Order request must not be null",
                    HttpStatus.BAD_REQUEST);
        }

        User currentUser = userRepository.findByEmailIgnoreCase("marketmaker@nggeen.com")
//        User currentUser = userRepository.findByEmailIgnoreCase("trader1@nggeen.com")
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.NOT_FOUND,
                        "User not found",
                        HttpStatus.NOT_FOUND));
        Account account = accountRepository.findByUserIdAndAccountTypeAndStatus(currentUser.getId(), AccountType.SPOT, AccountStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.NOT_FOUND,
                        "Account not found",
                        HttpStatus.NOT_FOUND));

        OrderBook instrumentOrderBook = orderBookRegistry.getByInstrumentSymbol(orderRequest.getSymbol());
        Instrument instrument = orderBookRegistry.getInstrumentBySymbol(orderRequest.getSymbol());

        Order incomingOrder = OrderMapper.toDomainOrder(orderRequest, UuidCreator.getTimeOrderedEpoch());
        incomingOrder.setAccountId(account.getId());
        incomingOrder.setUserId(currentUser.getId());
        incomingOrder.setBaseAsset(instrument.getBaseAsset());
        incomingOrder.setQuoteAsset(instrument.getQuoteAsset());

        instrumentValidator.validate(incomingOrder, instrument);

        boolean isBuy = orderRequest.getOrderSide() == OrderSide.BUY;
        String tokenToLock = isBuy ? instrument.getQuoteAsset() : instrument.getBaseAsset();

        BigDecimal amountToLock = calculateAmountToLock(isBuy, orderRequest, instrumentOrderBook);
        incomingOrder.setLockedAmount(amountToLock);

        accountBalanceService.reserveFunds(currentUser.getId(), amountToLock, tokenToLock);

        String symbol = incomingOrder.getSymbol();
        ExecutorService executor = orderBookRegistry.getExecutorFor(symbol);

        executor.submit(() -> processOrderSafely(
                instrumentOrderBook,
                incomingOrder,
                symbol,
                currentUser.getId(),
                amountToLock,
                tokenToLock));
    }

    public List<OrderResponse> getActiveOrders() {
        return orderBookRegistry.getAllOrderBooks().values().stream()
                .flatMap(orderBook -> orderBook.getActiveOrders().stream())
                .map(OrderMapper::toDTO)
                .toList();
    }

    private void processOrderSafely(
            OrderBook instrumentOrderBook,
            Order incomingOrder,
            String symbol,
            UUID userId,
            BigDecimal amountToRelease,
            String assetLocked) {
        try {
            long seq = instrumentOrderBook.getSequenceGenerator().next();

            DomainEvent acceptedEvent = incomingOrder.markAccepted(seq);
            long lastIdx = chronicleQueueService.appendEvent(EventType.OrderAccepted, acceptedEvent);

            instrumentOrderBook.processOrder(incomingOrder);

            if (shouldSnapshot(instrumentOrderBook)) {
                OrderBookSnapshot snapshot = instrumentOrderBook.captureSnapshot(lastIdx);
                fileSnapShotStore.save(snapshot);
            }

        } catch (Exception e) {
            log.error("Failed to process order {} on symbol {}", incomingOrder.getId(), symbol, e);
            accountBalanceService.releaseFunds(userId, amountToRelease, assetLocked);
            throw new NggeenException(
                    ErrorCode.ORDER_EXECUTION_FAILED,
                    "Order execution failed: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e
            );
        }
    }

    private boolean shouldSnapshot(OrderBook orderBook) {
        long current = orderBook.getSequenceGenerator().current();
        return current > 0 && (current % SNAPSHOT_INTERVAL == 0);
    }

    private BigDecimal calculateAmountToLock(boolean isBuy, PlaceOrderRequest request, OrderBook instrumentOrderbook) {
        if (!isBuy) {
            return request.getQuantity();
        }

        if (request.getOrderType() == OrderType.LIMIT) {
            return request.getPrice().multiply(request.getQuantity());
        }

        BigDecimal bestAsk = instrumentOrderbook.getLatestBestAsk();
        if (bestAsk == null) {
            throw new InvalidOrderException(
                    ErrorCode.INVALID_REQUEST,
                    "Cannot place market buy: No ask liquidity in order book",
                    HttpStatus.BAD_REQUEST
            );
        }
//        MAx lock cost for market buy is calculated using the best ask in orderbook. muliplies it with qunaitiy and and room for slippage(1+slippage room in percentage)
        BigDecimal baseCost = bestAsk.multiply(request.getQuantity());
        BigDecimal slippageBuffer = new BigDecimal("1.02");
        return baseCost.multiply(slippageBuffer);
    }
}
