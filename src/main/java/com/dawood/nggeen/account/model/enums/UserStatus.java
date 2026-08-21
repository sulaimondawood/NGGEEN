package com.dawood.nggeen.account.model.enums;

public enum UserStatus {
    ACTIVE,
    SUSPENDED,  // Cannot place new orders, can cancel
    FROZEN,     // Complete lock (cannot trade, cancel, or withdraw)
    CLOSED
}
