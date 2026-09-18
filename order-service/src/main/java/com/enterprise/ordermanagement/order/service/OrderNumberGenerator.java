package com.enterprise.ordermanagement.order.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class OrderNumberGenerator {

    public String generate() {
        String date = LocalDate.now().toString().replace("-", "");

        String suffix = UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();

        return "ORD-" + date + "-" + suffix;
    }
}
