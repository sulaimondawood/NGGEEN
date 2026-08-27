package com.dawood.nggeen.account.application;

import com.dawood.nggeen.account.infrastructure.persistence.AccountBalanceRepository;
import com.dawood.nggeen.account.model.AccountBalance;
import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.shared.exception.ResourceNotFoundException;
import com.dawood.nggeen.trade.event.OrderCancelled;
import com.dawood.nggeen.trade.event.TradeExecuted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerSettlementService {
    private final AccountBalanceRepository accountBalanceRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean processTradeExecution(TradeExecuted tradeExecuted) {
        BigDecimal price = tradeExecuted.getPrice();
        BigDecimal executedQty = tradeExecuted.getExecutedQuantity();

        UUID buyerAccountId = tradeExecuted.getBuyerAccountId();
        UUID sellerAccountId = tradeExecuted.getSellerAccountId();

        if (Objects.equals(buyerAccountId, sellerAccountId)) {
            log.warn("Self-trade detected for trade {}. Skipping ledger balance mutation.", tradeExecuted.getTradeId());
            return false;
        }

        String quoteAsset = tradeExecuted.getQuoteAsset();
        String baseAsset = tradeExecuted.getBaseAsset();
        BigDecimal quoteAmount = executedQty.multiply(price);

        AccountBalance buyerQuoteBalance;
        AccountBalance buyerBaseBalance;
        AccountBalance sellerQuoteBalance;
        AccountBalance sellerBaseBalance;

        if (buyerAccountId.compareTo(sellerAccountId) < 0) {
            buyerQuoteBalance = getBalanceForUpdate(buyerAccountId, quoteAsset);
            buyerBaseBalance = getBalanceForUpdate(buyerAccountId, baseAsset);
            sellerQuoteBalance = getBalanceForUpdate(sellerAccountId, quoteAsset);
            sellerBaseBalance = getBalanceForUpdate(sellerAccountId, baseAsset);
        } else {
            sellerQuoteBalance = getBalanceForUpdate(sellerAccountId, quoteAsset);
            sellerBaseBalance = getBalanceForUpdate(sellerAccountId, baseAsset);
            buyerQuoteBalance = getBalanceForUpdate(buyerAccountId, quoteAsset);
            buyerBaseBalance = getBalanceForUpdate(buyerAccountId, baseAsset);
        }


        buyerQuoteBalance.settleDeduction(quoteAmount);
        buyerBaseBalance.credit(executedQty);

        sellerBaseBalance.settleDeduction(executedQty);
        sellerQuoteBalance.credit(quoteAmount);

        accountBalanceRepository.save(buyerQuoteBalance);
        accountBalanceRepository.save(buyerBaseBalance);
        accountBalanceRepository.save(sellerBaseBalance);
        accountBalanceRepository.save(sellerQuoteBalance);

        log.debug("Settled trade {}: Buyer received {} {}, Seller received {} {}",
                tradeExecuted.getTradeId(), tradeExecuted.getExecutedQuantity(), tradeExecuted.getBaseAsset(), quoteAmount, tradeExecuted.getQuoteAsset());
        return true;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void processOrderCancellation(OrderCancelled orderCancelled) {
        BigDecimal amountToRelease = orderCancelled.getAmountToRelease();
        UUID accountId = orderCancelled.getAccountId();
        String lockedAsset = orderCancelled.getLockedAsset();

        AccountBalance accountBalance = accountBalanceRepository.findByAccountIdAndAsset(accountId, lockedAsset)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOT_FOUND,
                        String.format("Balance record not found for asset %s", lockedAsset),
                        HttpStatus.NOT_FOUND));

        accountBalance.releaseLockedFunds(amountToRelease);
        accountBalanceRepository.save(accountBalance);

        log.info("Released {} {} for cancelled order {}", amountToRelease, lockedAsset, orderCancelled.getOrderId());

    }

    private AccountBalance getBalanceForUpdate(UUID accountId, String asset) {
      return accountBalanceRepository.findByAccountIdAndAsset(accountId, asset)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOT_FOUND,
                        String.format("Balance record not found for asset %s", asset),
                        HttpStatus.NOT_FOUND));
    }
}
