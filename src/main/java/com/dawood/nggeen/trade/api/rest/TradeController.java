package com.dawood.nggeen.trade.api.rest;

import com.dawood.nggeen.shared.dto.ApiResponse;
import com.dawood.nggeen.trade.api.rest.dto.PlaceOrderRequest;
import com.dawood.nggeen.trade.application.TradeApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trades")
@RequiredArgsConstructor
public class TradeController {
    private final TradeApplicationService tradeService;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> placeOrder(@RequestBody @Valid PlaceOrderRequest request) {
        tradeService.processIncomingOrder(request);
        return ResponseEntity.accepted().body(ApiResponse.successMessage("Your order has been submitted"));
    }

}
