package com.aidataanalyst.subscriptions.api;

public record BillingResponse(String planCode, String status, String message) {
}
