package com.dawood.nggeen.trade.api.rest;

import com.dawood.nggeen.trade.api.rest.dto.PlaceOrderRequest;
import com.dawood.nggeen.trade.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trades")
@RequiredArgsConstructor
public class OrderController {
private final TradeService tradeService;

    @PostMapping
    public ResponseEntity<String> placeOrder(@RequestBody PlaceOrderRequest request){
        tradeService.processIncomingOrder(request);
        return ResponseEntity.ok().body("Order placed");
    }

}
