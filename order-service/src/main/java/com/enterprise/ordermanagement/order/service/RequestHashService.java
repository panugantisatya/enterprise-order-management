package com.enterprise.ordermanagement.order.service;

import com.enterprise.ordermanagement.order.dto.CreateOrderRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class RequestHashService {

    public String hash(CreateOrderRequest request) {

        StringBuilder content = new StringBuilder();

        content.append(request.customerId())
                .append("|")
                .append(request.currency())
                .append("|");

        request.items().forEach(item ->
                content.append(item.productId())
                        .append("|")
                        .append(item.quantity())
                        .append("|")
                        .append(item.unitPrice())
                        .append("|")
        );

        return sha256(content.toString());
    }

    private String sha256(String value) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            value.getBytes(StandardCharsets.UTF_8)
                    );

            StringBuilder result = new StringBuilder();

            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }

            return result.toString();

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception
            );
        }
    }
}
