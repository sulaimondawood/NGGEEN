package com.dawood.nggeen.account.api.rest;

import com.dawood.nggeen.account.api.rest.dto.BalanceResponse;
import com.dawood.nggeen.account.api.rest.dto.DemoDepositRequest;
import com.dawood.nggeen.account.application.AccountBalanceService;
import com.dawood.nggeen.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountBalanceService accountBalanceService;

    @PostMapping("/demo-deposit")
    public ResponseEntity<ApiResponse<BalanceResponse>> demoDeposit(@Valid @RequestBody DemoDepositRequest request) {
        BalanceResponse response = accountBalanceService.demoDeposit(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Demo funds credited"));
    }

    @GetMapping("/balances")
    public ResponseEntity<ApiResponse<List<BalanceResponse>>> balances() {
        return ResponseEntity.ok(
                ApiResponse.success(accountBalanceService.getBalances(), "Balances fetched")
        );
    }
}
