package com.example.mondecole_pocket.entity.enums;


public enum SubscriptionPlan {
    FREE(50, 1000),
    BASIC(200, 10000),
    PROFESSIONAL(1000, 50000),
    ENTERPRISE(-1, -1);

    private final int maxUsers;
    private final long maxStorageMb;

    SubscriptionPlan(int maxUsers, long maxStorageMb) {
        this.maxUsers = maxUsers;
        this.maxStorageMb = maxStorageMb;
    }

    public int getMaxUsers() {
        return maxUsers;
    }

    public long getMaxStorageMb() {
        return maxStorageMb;
    }

    public boolean isUnlimited() {
        return maxUsers == -1;
    }
}