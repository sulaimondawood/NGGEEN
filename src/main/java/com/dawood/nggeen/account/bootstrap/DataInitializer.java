package com.dawood.nggeen.account.bootstrap;

import com.dawood.nggeen.shared.infrastructure.persistence.AccountBalanceRepository;
import com.dawood.nggeen.shared.infrastructure.persistence.AccountRepository;
import com.dawood.nggeen.shared.infrastructure.persistence.UserRepository;
import com.dawood.nggeen.account.model.Account;
import com.dawood.nggeen.account.model.AccountBalance;
import com.dawood.nggeen.account.model.User;
import com.dawood.nggeen.account.model.enums.AccountStatus;
import com.dawood.nggeen.account.model.enums.AccountType;
import com.dawood.nggeen.account.model.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@Profile("!prod") // Runs in dev/test, skipped in production
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Temporary seed data already exists. Skipping initialization.");
            return;
        }

        log.info("Seeding temporary mock data for Nggeen exchange...");

        // 1. Create Active Traders & Admins
        String defaultPasswordHash ="Password123!";

        User trader1 = User.create(
                "trader1@nggeen.com",
                "Dauda Sulaimon",
                defaultPasswordHash,
                UserStatus.ACTIVE
        );

        User trader2 = User.create(
                "marketmaker@nggeen.com",
                "Jane Doe",
                defaultPasswordHash,
                UserStatus.ACTIVE
        );

        User suspendedUser = User.create(
                "newbie@nggeen.com",
                "Alex Smith",
                defaultPasswordHash,
                UserStatus.SUSPENDED
        );

        userRepository.saveAll(List.of(trader1, trader2, suspendedUser));

        // 2. Create Accounts (Spot Wallets)
        Account acc1 = Account.builder()
                .user(trader1)
                .accountType(AccountType.SPOT)
                .status(AccountStatus.ACTIVE)
                .build();

        Account acc2 = Account.builder()
                .user(trader2)
                .accountType(AccountType.SPOT)
                .status(AccountStatus.ACTIVE)
                .build();

        accountRepository.saveAll(List.of(acc1, acc2));

        // 3. Create Multi-Asset Balances
        List<AccountBalance> balances = List.of(
                // Trader 1 Balances
                new AccountBalance(acc1, "USDT", new BigDecimal("50000.000000000000000000"), BigDecimal.ZERO),
                new AccountBalance(acc1, "BTC", new BigDecimal("2.500000000000000000"), BigDecimal.ZERO),
                new AccountBalance(acc1, "ETH", new BigDecimal("15.000000000000000000"), BigDecimal.ZERO),
                new AccountBalance(acc1, "NGN", new BigDecimal("25000000.000000000000000000"), BigDecimal.ZERO),

                // Market Maker Balances (High Liquidity)
                new AccountBalance(acc2, "USDT", new BigDecimal("1000000.000000000000000000"), BigDecimal.ZERO),
                new AccountBalance(acc2, "BTC", new BigDecimal("50.000000000000000000"), BigDecimal.ZERO),
                new AccountBalance(acc2, "ETH", new BigDecimal("200.000000000000000000"), BigDecimal.ZERO)
        );

        accountBalanceRepository.saveAll(balances);

        log.info("Successfully seeded 3 users, 2 accounts, and {} asset balance rows.", balances.size());
    }
}