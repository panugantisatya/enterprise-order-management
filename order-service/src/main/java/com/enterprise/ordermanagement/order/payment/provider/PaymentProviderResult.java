package com.enterprise.ordermanagement.order.payment.provider;

public record PaymentProviderResult(
        boolean successful,
        String providerPaymentId,
        String failureReason
) {

    public static PaymentProviderResult success(String providerPaymentId) {
        return new PaymentProviderResult(
                true,
                providerPaymentId,
                null
        );
    }

    public static PaymentProviderResult failure(String failureReason) {
        return new PaymentProviderResult(
                false,
                null,
                failureReason
        );
    }
}
