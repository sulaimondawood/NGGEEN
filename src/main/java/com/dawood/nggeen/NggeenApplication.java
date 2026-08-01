package com.dawood.nggeen;

import com.dawood.nggeen.domain.Order;
import com.dawood.nggeen.domain.OrderBook;
import com.dawood.nggeen.domain.enums.OrderStatus;
import com.dawood.nggeen.domain.enums.OrderType;
import com.dawood.nggeen.domain.enums.Side;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.UUID;

@SpringBootApplication
public class NggeenApplication {

    public static void main(String[] args) {
        SpringApplication.run(NggeenApplication.class, args);
    }

    @Bean
    public CommandLineRunner test(){
        return args -> {
            OrderBook book = new OrderBook();

            Order sell1 = createTestOrder(Side.SELL, OrderType.LIMIT, "100.00", "5", "user_1");
            Order sell2 = createTestOrder(Side.SELL, OrderType.LIMIT, "100.00", "3", "user_2");
            Order sell3 = createTestOrder(Side.SELL, OrderType.LIMIT, "102.00", "2", "user_3");
            Order sell4 = createTestOrder(Side.SELL, OrderType.LIMIT, "104.00", "5", "user_4");
            Order sell5 = createTestOrder(Side.SELL, OrderType.LIMIT, "101.00", "3", "user_5");

            book.processOrder(sell1);
            book.processOrder(sell2);
            book.processOrder(sell3);
            book.processOrder(sell4);
            book.processOrder(sell5);

            Order buyer = createTestOrder(Side.BUY, OrderType.LIMIT, "100.00", "7", "user_6");
            Order buyer2 = createTestOrder(Side.BUY, OrderType.LIMIT, "101.01", "3", "user_7");
            Order buyer3 = createTestOrder(Side.BUY, OrderType.LIMIT, "105.00", "8", "user_8");

            book.processOrder(buyer);
            book.processOrder(buyer2);
            book.processOrder(buyer3);


            System.out.println("=== Order Matching Summary ===");
            System.out.println("Best Ask: " + book.getBestAskPrice());
            System.out.println("Best Bid: " + book.getBestBidPrice());

            System.out.println("\n--- All Submitted Sellers Status ---");
            System.out.printf("sell1 (%s @ 100.00): %s (Remaining: %s)%n", sell1.getUserId(), sell1.getStatus(), sell1.getRemainingQuantity());
            System.out.printf("sell2 (%s @ 100.00): %s (Remaining: %s)%n", sell2.getUserId(), sell2.getStatus(), sell2.getRemainingQuantity());
            System.out.printf("sell3 (%s @ 102.00): %s (Remaining: %s)%n", sell3.getUserId(), sell3.getStatus(), sell3.getRemainingQuantity());
            System.out.printf("sell4 (%s @ 104.00): %s (Remaining: %s)%n", sell4.getUserId(), sell4.getStatus(), sell4.getRemainingQuantity());
            System.out.printf("sell5 (%s @ 101.00): %s (Remaining: %s)%n", sell5.getUserId(), sell5.getStatus(), sell5.getRemainingQuantity());

            System.out.println("\n--- All Submitted Buyers Status ---");
            System.out.printf("buyer1 (%s @ 100.00): %s (Remaining: %s)%n", buyer.getUserId(), buyer.getStatus(), buyer.getRemainingQuantity());
            System.out.printf("buyer2 (%s @ 101.01): %s (Remaining: %s)%n", buyer2.getUserId(), buyer2.getStatus(), buyer2.getRemainingQuantity());
            System.out.printf("buyer3 (%s @ 105.00): %s (Remaining: %s)%n", buyer3.getUserId(), buyer3.getStatus(), buyer3.getRemainingQuantity());

            System.out.println("\n--- Orders Still Resting in Asks Tree ---");
            for (LinkedList<Order> priceQueue : book.getAsks().values()) {
                for (Order seller : priceQueue) {
                    System.out.printf("Price [%s] - User [%s]: %s (Qty: %s)%n",
                            seller.getPrice(), seller.getUserId(), seller.getStatus(), seller.getRemainingQuantity());
                }
            }

            System.out.println("\n--- Orders Still Resting in Bids Tree ---");
            for (LinkedList<Order> priceQueue : book.getBids().values()) {
                for (Order b : priceQueue) {
                    System.out.printf("Price [%s] - User [%s]: %s (Qty: %s)%n",
                            b.getPrice(), b.getUserId(), b.getStatus(), b.getRemainingQuantity());
                }
            }

        };
    }

    private Order createTestOrder(Side side, OrderType type, String price, String qty, String userId) {
        UUID orderId = UUID.randomUUID();
        return new Order(
                UUID.randomUUID(),
                orderId,
                side,
                type,
                new BigDecimal(price),
                new BigDecimal(qty),
                BigDecimal.ZERO,
                OrderStatus.NEW,
                LocalDateTime.now(),
                userId
        );
    }


}
