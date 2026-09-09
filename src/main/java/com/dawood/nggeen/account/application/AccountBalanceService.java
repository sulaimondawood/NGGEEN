package com.dawood.nggeen.account.application;

import com.dawood.nggeen.account.api.rest.dto.BalanceResponse;
import com.dawood.nggeen.account.api.rest.dto.DemoDepositRequest;
import com.dawood.nggeen.account.infrastructure.persistence.AccountBalanceRepository;
import com.dawood.nggeen.account.infrastructure.persistence.AccountRepository;
import com.dawood.nggeen.account.model.Account;
import com.dawood.nggeen.account.model.AccountBalance;
import com.dawood.nggeen.account.model.enums.AccountStatus;
import com.dawood.nggeen.account.model.enums.AccountType;
import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.shared.exception.BadRequestException;
import com.dawood.nggeen.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountBalanceService {
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountRepository accountRepository;

    private static final Set<String> SUPPORTED_ASSETS = Set.of("USDT", "BTC");

    private static final Map<String, BigDecimal> MAX_PER_TX = Map.of(
            "USDT", new BigDecimal("100000"),
            "BTC", new BigDecimal("10")
    );

    private static final Map<String, BigDecimal> MAX_BALANCE = Map.of(
            "USDT", new BigDecimal("1000000"),
            "BTC", new BigDecimal("50")
    );

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void reserveFunds(UUID userId, BigDecimal amountToReserve, String asset) {
        Account account = accountRepository.findByUserIdAndAccountTypeAndStatus(userId, AccountType.SPOT, AccountStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOT_FOUND,
                        "Account not found",
                        HttpStatus.NOT_FOUND));

        AccountBalance balance = accountBalanceRepository.findByAccountIdAndAsset(account.getId(), asset)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOT_FOUND,
                        String.format("Balance record not found for asset %s", asset),
                        HttpStatus.NOT_FOUND));

        balance.lockFunds(amountToReserve);

        accountBalanceRepository.save(balance);

        log.debug("Reserved {} {} for account {}", amountToReserve, asset, account.getId());
    }

    @Transactional
    public void releaseFunds(UUID userId, BigDecimal amountToReserve, String asset){
        Account account = accountRepository.findByUserIdAndAccountTypeAndStatus(userId, AccountType.SPOT, AccountStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOT_FOUND,
                        "Account not found",
                        HttpStatus.NOT_FOUND));

        AccountBalance balance = accountBalanceRepository.findByAccountIdAndAsset(account.getId(), asset)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOT_FOUND,
                        String.format("Balance record not found for asset %s", asset),
                        HttpStatus.NOT_FOUND));

        balance.releaseLockedFunds(amountToReserve);

        accountBalanceRepository.save(balance);

        log.debug("Released {} {} for account {}", amountToReserve, asset, account.getId());
    }

    @Transactional
    public BalanceResponse demoDeposit(UUID userId, DemoDepositRequest request) {
        String asset = request.asset().trim().toUpperCase();
        BigDecimal amount = request.amount();

        validateAsset(asset);
        validateAmount(asset, amount);

        Account account = accountRepository.findByUserIdAndAccountTypeAndStatus(userId, AccountType.SPOT, AccountStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.NOT_FOUND,
                        "Spot account not found",
                        HttpStatus.NOT_FOUND
                ));

        AccountBalance balance = accountBalanceRepository
                .findByAccountIdAndAsset(account.getId(), asset)
                .orElseGet(() -> AccountBalance.builder()
                        .account(account)
                        .asset(asset)
                        .available(BigDecimal.ZERO)
                        .reserved(BigDecimal.ZERO)
                        .build());

        BigDecimal currentAvailable = balance.getAvailable() != null
                ? balance.getAvailable()
                : BigDecimal.ZERO;

        BigDecimal projected = currentAvailable.add(amount);
        BigDecimal maxBalance = MAX_BALANCE.get(asset);
        if (maxBalance != null && projected.compareTo(maxBalance) > 0) {
            throw new BadRequestException(
                    ErrorCode.BAD_REQUEST,
                    "Demo balance cap exceeded for " + asset,
                    HttpStatus.BAD_REQUEST
            );
        }

        balance.credit(amount);
        accountBalanceRepository.save(balance);

        return new BalanceResponse(
                asset,
                balance.getAvailable(),
                balance.getReserved() != null ? balance.getReserved() : BigDecimal.ZERO
        );
    }

    @Transactional(readOnly = true)
    public java.util.List<BalanceResponse> getBalances(UUID userId) {
        Account account = accountRepository.findByUserIdAndAccountTypeAndStatus(userId, AccountType.SPOT, AccountStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.NOT_FOUND,
                        "Spot account not found",
                        HttpStatus.NOT_FOUND
                ));

        return accountBalanceRepository.findAllByAccountId(account.getId())
                .stream()
                .map(b -> new BalanceResponse(
                        b.getAsset(),
                        b.getAvailable() != null ? b.getAvailable() : BigDecimal.ZERO,
                        b.getReserved() != null ? b.getReserved() : BigDecimal.ZERO
                ))
                .toList();
    }

    private void validateAsset(String asset) {
        if (!SUPPORTED_ASSETS.contains(asset)) {
            throw new BadRequestException(
                    ErrorCode.BAD_REQUEST,
                    "Unsupported asset. Allowed: " + SUPPORTED_ASSETS,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validateAmount(String asset, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(
                    ErrorCode.BAD_REQUEST,
                    "Amount must be greater than zero",
                    HttpStatus.BAD_REQUEST
            );
        }

        BigDecimal maxPerTx = MAX_PER_TX.get(asset);
        if (maxPerTx != null && amount.compareTo(maxPerTx) > 0) {
            throw new BadRequestException(
                    ErrorCode.BAD_REQUEST,
                    "Amount exceeds demo per-transaction limit for " + asset,
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
