package com.fooddelivery.delivery.service;

import com.fooddelivery.delivery.entity.DeliveryPartner;
import com.fooddelivery.delivery.entity.DeliveryPartnerStatus;
import com.fooddelivery.delivery.repository.DeliveryPartnerRepository;
import com.fooddelivery.delivery.service.redis.DeliveryPartnerGeoRedisService;
import com.fooddelivery.delivery.service.redis.DeliveryPartnerRedisService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryPartnerServiceImplTest {

    private static final Long PARTNER_ID = 42L;

    @Mock
    private DeliveryPartnerRepository repository;

    @Mock
    private DeliveryPartnerRedisService redisService;

    @Mock
    private DeliveryPartnerGeoRedisService geoRedisService;

    @InjectMocks
    private DeliveryPartnerServiceImpl service;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void updateAvailabilityAppliesRedisProjectionOnlyAfterCommit() {
        DeliveryPartner partner = partner(DeliveryPartnerStatus.OFFLINE);
        when(repository.findById(PARTNER_ID)).thenReturn(Optional.of(partner));
        when(repository.save(partner)).thenReturn(partner);

        service.updateAvailability(PARTNER_ID, true);

        verify(repository).save(partner);
        verifyNoInteractions(redisService, geoRedisService);

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        verify(redisService).addAvailablePartner(PARTNER_ID);
        verifyNoInteractions(geoRedisService);
    }

    @Test
    void rolledBackAvailabilityUpdateDoesNotChangeRedisProjection() {
        DeliveryPartner partner = partner(DeliveryPartnerStatus.OFFLINE);
        when(repository.findById(PARTNER_ID)).thenReturn(Optional.of(partner));
        when(repository.save(partner)).thenReturn(partner);

        service.updateAvailability(PARTNER_ID, true);

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization ->
                        synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verifyNoInteractions(redisService, geoRedisService);
    }

    @Test
    void deleteRemovesRedisProjectionsOnlyAfterCommit() {
        when(repository.findById(PARTNER_ID))
                .thenReturn(Optional.of(partner(DeliveryPartnerStatus.AVAILABLE)));

        service.deleteDeliveryPartner(PARTNER_ID);

        verify(repository).delete(any(DeliveryPartner.class));
        verifyNoInteractions(redisService, geoRedisService);

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        verify(geoRedisService).removeLocation(PARTNER_ID);
        verify(redisService).removeAvailablePartner(PARTNER_ID);
    }

    @Test
    void failedDeleteDoesNotRemoveRedisProjections() {
        DeliveryPartner partner = partner(DeliveryPartnerStatus.AVAILABLE);
        when(repository.findById(PARTNER_ID)).thenReturn(Optional.of(partner));
        doThrow(new DataIntegrityViolationException("delete failed"))
                .when(repository).delete(partner);

        assertThrows(DataIntegrityViolationException.class,
                () -> service.deleteDeliveryPartner(PARTNER_ID));

        verifyNoInteractions(redisService, geoRedisService);
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
    }

    private DeliveryPartner partner(DeliveryPartnerStatus status) {
        return DeliveryPartner.builder()
                .id(PARTNER_ID)
                .userId(17L)
                .status(status)
                .build();
    }
}
