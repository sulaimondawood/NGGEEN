package com.dawood.nggeen.account.application;

import com.dawood.nggeen.shared.infrastructure.persistence.AccountBalanceRepository;
import com.dawood.nggeen.shared.infrastructure.persistence.AccountRepository;
import com.dawood.nggeen.account.model.Account;
import com.dawood.nggeen.account.model.AccountBalance;
import com.dawood.nggeen.account.model.enums.AccountStatus;
import com.dawood.nggeen.account.model.enums.AccountType;
import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountBalanceService {
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountRepository accountRepository;

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
}
