package com.dawood.nggeen.trade.api.rest;

import com.dawood.nggeen.shared.dto.ApiResponse;
import com.dawood.nggeen.trade.api.rest.dto.CancelOrderRequest;
import com.dawood.nggeen.trade.api.rest.dto.OrderResponse;
import com.dawood.nggeen.trade.api.rest.dto.PlaceOrderRequest;
import com.dawood.nggeen.trade.application.TradeApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final TradeApplicationService tradeService;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> placeOrder(@RequestBody @Valid PlaceOrderRequest request) {
        tradeService.processIncomingOrder(request);
        return ResponseEntity.accepted().body(ApiResponse.successMessage("Your order has been submitted"));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getActiveOrders() {
        return ResponseEntity.ok(ApiResponse.success(tradeService.getActiveOrders()));
    }

    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> cancelOrder(@Valid @RequestBody CancelOrderRequest request) {
        tradeService.cancelRestingOrder(request);
        return ResponseEntity.accepted().body(ApiResponse.successMessage("Your request has been submitted"));
    }

}
