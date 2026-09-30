package com.enterprise.ordermanagement.order.bulk;

import com.enterprise.ordermanagement.order.entity.BulkOrderJobItem;
import com.enterprise.ordermanagement.order.repository.BulkOrderJobItemRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BulkOrderJobItemServiceTest {

    @Test
    void shouldCreateAndClaimNewItem() {

        BulkOrderJobItemRepository repository =
                mock(BulkOrderJobItemRepository.class);

        JsonMapper jsonMapper =
                new JsonMapper();

        UUID jobId = UUID.randomUUID();

        BulkOrderJobItemService service =
                new BulkOrderJobItemService(
                        repository,
                        jsonMapper
                );

        boolean claimed =
                service.claimItem(jobId, 0);

        assertFalse(claimed);

        verify(repository)
                .findByJobIdAndItemIndex(jobId, 0);
    }

    @Test
    void shouldRejectAlreadySucceededItem() {

        BulkOrderJobItemRepository repository =
                mock(BulkOrderJobItemRepository.class);

        JsonMapper jsonMapper =
                new JsonMapper();

        UUID jobId = UUID.randomUUID();

        BulkOrderJobItem item =
                new BulkOrderJobItem(
                        jobId,
                        0,
                        "{\"customerId\":\"11111111-1111-1111-1111-111111111111\"}"
                );

        item.markSucceeded(UUID.randomUUID());

        when(repository.findByJobIdAndItemIndex(jobId, 0))
                .thenReturn(Optional.of(item));

        BulkOrderJobItemService service =
                new BulkOrderJobItemService(
                        repository,
                        jsonMapper
                );

        boolean claimed =
                service.claimItem(jobId, 0);

        assertFalse(claimed);

        verify(repository)
                .findByJobIdAndItemIndex(jobId, 0);
    }

    @Test
    void shouldRejectAlreadyFailedItem() {

        BulkOrderJobItemRepository repository =
                mock(BulkOrderJobItemRepository.class);

        JsonMapper jsonMapper =
                new JsonMapper();

        UUID jobId = UUID.randomUUID();

        BulkOrderJobItem item =
                new BulkOrderJobItem(
                        jobId,
                        0,
                        "{\"customerId\":\"11111111-1111-1111-1111-111111111111\"}"
                );

        item.markFailed("Invalid order");

        when(repository.findByJobIdAndItemIndex(jobId, 0))
                .thenReturn(Optional.of(item));

        BulkOrderJobItemService service =
                new BulkOrderJobItemService(
                        repository,
                        jsonMapper
                );

        boolean claimed =
                service.claimItem(jobId, 0);

        assertFalse(claimed);
    }

    @Test
    void shouldReturnFalseWhenItemDoesNotExist() {

        BulkOrderJobItemRepository repository =
                mock(BulkOrderJobItemRepository.class);

        JsonMapper jsonMapper =
                new JsonMapper();

        UUID jobId = UUID.randomUUID();

        when(repository.findByJobIdAndItemIndex(jobId, 0))
                .thenReturn(Optional.empty());

        BulkOrderJobItemService service =
                new BulkOrderJobItemService(
                        repository,
                        jsonMapper
                );

        boolean claimed =
                service.claimItem(jobId, 0);

        assertFalse(claimed);
    }

    @Test
    void shouldMarkSucceededItem() {

        BulkOrderJobItemRepository repository =
                mock(BulkOrderJobItemRepository.class);

        JsonMapper jsonMapper =
                new JsonMapper();

        UUID jobId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        BulkOrderJobItem item =
                new BulkOrderJobItem(
                        jobId,
                        0,
                        "{\"currency\":\"INR\"}"
                );

        when(repository.findByJobIdAndItemIndex(jobId, 0))
                .thenReturn(Optional.of(item));

        BulkOrderJobItemService service =
                new BulkOrderJobItemService(
                        repository,
                        jsonMapper
                );

        service.markSucceeded(
                jobId,
                0,
                orderId
        );

        assertTrue(
                item.getStatus()
                        == BulkOrderJobItem.BulkOrderJobItemStatus.SUCCEEDED
        );

        verify(repository)
                .findByJobIdAndItemIndex(jobId, 0);
    }

    @Test
    void shouldMarkFailedItem() {

        BulkOrderJobItemRepository repository =
                mock(BulkOrderJobItemRepository.class);

        JsonMapper jsonMapper =
                new JsonMapper();

        UUID jobId = UUID.randomUUID();

        BulkOrderJobItem item =
                new BulkOrderJobItem(
                        jobId,
                        0,
                        "{\"currency\":\"INR\"}"
                );

        when(repository.findByJobIdAndItemIndex(jobId, 0))
                .thenReturn(Optional.of(item));

        BulkOrderJobItemService service =
                new BulkOrderJobItemService(
                        repository,
                        jsonMapper
                );

        service.markFailed(
                jobId,
                0,
                "Invalid order"
        );

        assertTrue(
                item.getStatus()
                        == BulkOrderJobItem.BulkOrderJobItemStatus.FAILED
        );

        verify(repository)
                .findByJobIdAndItemIndex(jobId, 0);
    }
}
