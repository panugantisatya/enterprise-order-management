package com.enterprise.ordermanagement.order.bulk;

import com.enterprise.ordermanagement.order.entity.BulkOrderJobItem;
import com.enterprise.ordermanagement.order.repository.BulkOrderJobItemRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class BulkOrderJobItemServiceTest {

    @Test
    void shouldClaimNewItem() {

        BulkOrderJobItemRepository repository =
                mock(BulkOrderJobItemRepository.class);

        UUID jobId = UUID.randomUUID();

        when(repository.findByJobIdAndItemIndex(jobId, 0))
                .thenReturn(Optional.empty());

        BulkOrderJobItemService service =
                new BulkOrderJobItemService(repository);

        boolean claimed =
                service.claimItem(jobId, 0);

        assertTrue(claimed);

        verify(repository).saveAndFlush(any(BulkOrderJobItem.class));
    }

    @Test
    void shouldRejectAlreadyClaimedItem() {

        BulkOrderJobItemRepository repository =
                mock(BulkOrderJobItemRepository.class);

        UUID jobId = UUID.randomUUID();

        when(repository.findByJobIdAndItemIndex(jobId, 0))
                .thenReturn(
                        Optional.of(
                                new BulkOrderJobItem(jobId, 0)
                        )
                );

        BulkOrderJobItemService service =
                new BulkOrderJobItemService(repository);

        boolean claimed =
                service.claimItem(jobId, 0);

        assertFalse(claimed);

        verify(repository, never())
                .saveAndFlush(any(BulkOrderJobItem.class));
    }
}
