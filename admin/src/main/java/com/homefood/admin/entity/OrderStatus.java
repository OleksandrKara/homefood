package com.homefood.admin.entity;

public enum OrderStatus {
    /** Submitted by a customer via the public /shop page — not yet confirmed by the business. */
    REQUESTED,
    NEW,
    DONE,
    CANCELLED
}
