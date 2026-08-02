package com.dawood.nggeen.trade.model;

import com.dawood.nggeen.trade.matching.OrderMatchingStrategy;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.TreeMap;

public class OrderBook {
    private OrderMatchingStrategy orderMatchingStrategy;

    private TreeMap<BigDecimal, LinkedList<Order>> bids = new TreeMap<>(Comparator.reverseOrder());
    private TreeMap<BigDecimal, LinkedList<Order>> asks = new TreeMap<>();


    private BigDecimal getBestBid(){
      return bids.isEmpty()? null:  bids.firstKey();
    }

    private BigDecimal getBestAsk(){
        return asks.isEmpty()? null: asks.firstKey();
    }
}
