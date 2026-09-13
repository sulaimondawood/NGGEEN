package com.dawood.nggeen.account.application;

import com.dawood.nggeen.account.infrastructure.persistence.AccountBalanceRepository;
import com.dawood.nggeen.account.infrastructure.persistence.AccountRepository;
import com.dawood.nggeen.account.model.Account;
import com.dawood.nggeen.account.model.AccountBalance;
import com.dawood.nggeen.account.model.enums.AccountStatus;
import com.dawood.nggeen.account.model.enums.AccountType;
import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.shared.exception.ResourceNotFoundException;
import com.dawood.nggeen.trade.event.OrderCancelled;
import com.dawood.nggeen.trade.event.TradeExecuted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final AccountRepository accountRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean processTradeExecution(TradeExecuted tradeExecuted) {
        UUID buyerAccountId = tradeExecuted.getBuyerAccountId();
        UUID sellerAccountId = tradeExecuted.getSellerAccountId();

        log.error(buyerAccountId.toString());
        log.error(sellerAccountId.toString());

        if (Objects.equals(buyerAccountId, sellerAccountId)) {
            log.warn("Self-trade detected for trade {}. Skipping ledger balance mutation.", tradeExecuted.getTradeId());
            return false;
        }

        Account buyerAccount = accountRepository.findByIdAndAccountTypeAndStatus(buyerAccountId, AccountType.SPOT, AccountStatus.ACTIVE)
                .orElseThrow();

        Account sellerAccount = accountRepository.findByIdAndAccountTypeAndStatus(sellerAccountId, AccountType.SPOT, AccountStatus.ACTIVE)
                .orElseThrow();

        BigDecimal price = tradeExecuted.getPrice();
        BigDecimal executedQty = tradeExecuted.getExecutedQuantity();
        String quoteAsset = tradeExecuted.getQuoteAsset();
        String baseAsset = tradeExecuted.getBaseAsset();
        BigDecimal quoteAmount = executedQty.multiply(price);

        AccountBalance buyerQuoteBalance;
        AccountBalance buyerBaseBalance;
        AccountBalance sellerQuoteBalance;
        AccountBalance sellerBaseBalance;

        if (buyerAccountId.compareTo(sellerAccountId) < 0) {
            buyerQuoteBalance = getBalanceForUpdate(buyerAccountId, quoteAsset);
            buyerBaseBalance = getOrCreateBalanceForUpdate(buyerAccount, baseAsset);
            sellerBaseBalance = getBalanceForUpdate(sellerAccountId, baseAsset);
            sellerQuoteBalance = getOrCreateBalanceForUpdate(sellerAccount, quoteAsset);
        } else {
            sellerBaseBalance = getBalanceForUpdate(sellerAccountId, baseAsset);
            sellerQuoteBalance = getOrCreateBalanceForUpdate(sellerAccount, quoteAsset);
            buyerQuoteBalance = getBalanceForUpdate(buyerAccountId, quoteAsset);
            buyerBaseBalance = getOrCreateBalanceForUpdate(buyerAccount, baseAsset);
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

    private AccountBalance getOrCreateBalanceForUpdate(Account account, String asset) {
        return accountBalanceRepository.findByAccountIdAndAsset(account.getId(), asset)
                .orElseGet(() -> {
                    try {
                        return accountBalanceRepository.saveAndFlush(
                                AccountBalance.builder()
                                        .account(account)
                                        .asset(asset)
                                        .available(BigDecimal.ZERO)
                                        .reserved(BigDecimal.ZERO)
                                        .build()
                        );
                    } catch (DataIntegrityViolationException e) {
                        // Another concurrent thread inserted it first -> re-fetch with lock
                        return accountBalanceRepository.findByAccountIdAndAsset(account.getId(), asset)
                                .orElseThrow(() -> new IllegalStateException("Failed to resolve balance for " + asset, e));
                    }
                });
    }
}
