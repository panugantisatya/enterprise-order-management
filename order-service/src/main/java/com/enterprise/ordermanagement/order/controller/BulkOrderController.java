package com.enterprise.ordermanagement.order.controller;

import com.enterprise.ordermanagement.order.dto.BulkCreateOrderRequest;
import com.enterprise.ordermanagement.order.dto.BulkOrderJobResponse;
import com.enterprise.ordermanagement.order.service.BulkOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bulk-orders")
public class BulkOrderController {

    private final BulkOrderService bulkOrderService;

    public BulkOrderController(
            BulkOrderService bulkOrderService
    ) {
        this.bulkOrderService = bulkOrderService;
    }

    @PostMapping
    public ResponseEntity<BulkOrderJobResponse> createBulkOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody BulkCreateOrderRequest request
    ) {
        BulkOrderJobResponse response =
                bulkOrderService.createBulkOrder(
                        idempotencyKey,
                        request
                );

        return ResponseEntity
                .accepted()
                .body(response);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<BulkOrderJobResponse> getBulkOrderJob(
            @PathVariable UUID jobId
    ) {
        return ResponseEntity.ok(
                bulkOrderService.getBulkOrderJob(jobId)
        );
    }
}
