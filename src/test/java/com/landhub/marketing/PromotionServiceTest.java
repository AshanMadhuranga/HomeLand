package com.landhub.marketing;

import com.landhub.land.Land;
import com.landhub.land.LandService;
import com.landhub.land.LandStatus;
import com.landhub.verification.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private LandService landService;

    @Mock
    private VerificationService verificationService;

    private PromotionService promotionService;
    private Land land;

    @BeforeEach
    void setUp() {
        promotionService = new PromotionService(promotionRepository, landService, verificationService);
        land = new Land();
        land.setId(1L);
        land.setTitle("Verified Plot");
        land.setPrice(new BigDecimal("1000000.00"));
        land.setStatus(LandStatus.AVAILABLE);
        lenient().when(landService.findById(1L)).thenReturn(Optional.of(land));
        lenient().when(verificationService.hasApprovedVerification(1L)).thenReturn(true);
        lenient().when(promotionRepository.findAll()).thenReturn(List.of());
        lenient().when(promotionRepository.save(any(Promotion.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void calculatesPercentagePromotionalPriceWithoutChangingLandPrice() {
        Promotion promotion = promotion(PromotionStatus.ACTIVE);
        promotion.setDiscountPercentage(new BigDecimal("10.00"));

        assertEquals(new BigDecimal("900000.00"), promotion.getPromotionalPrice());
        assertEquals(new BigDecimal("1000000.00"), land.getPrice());
    }

    @Test
    void fixedDiscountCannotMakePriceNegative() {
        Promotion promotion = promotion(PromotionStatus.ACTIVE);
        promotion.setDiscountAmount(new BigDecimal("1500000.00"));

        assertEquals(new BigDecimal("0.00"), promotion.getPromotionalPrice());
    }

    @Test
    void rejectsBothDiscountModels() {
        assertThrows(IllegalArgumentException.class, () -> promotionService.create(
                "Offer", null, PromotionType.DISCOUNT, PromotionStatus.ACTIVE, 1L,
                new BigDecimal("10"), new BigDecimal("100"), null, false,
                LocalDate.now(), LocalDate.now().plusDays(5), null, null));
    }

    @Test
    void rejectsFixedDiscountThatReachesOriginalPrice() {
        assertThrows(IllegalArgumentException.class, () -> promotionService.create(
                "Offer", null, PromotionType.DISCOUNT, PromotionStatus.ACTIVE, 1L,
                null, new BigDecimal("1000000.00"), null, false,
                LocalDate.now(), LocalDate.now().plusDays(5), null, null));
    }

    @Test
    void createsVerifiedAvailableActivePromotion() {
        Promotion saved = promotionService.create(
                "Launch Offer", "Details", PromotionType.SPECIAL_OFFER, PromotionStatus.ACTIVE, 1L,
                null, new BigDecimal("50000"), "Save today", true,
                LocalDate.now(), LocalDate.now().plusDays(5), null, null);

        assertEquals(PromotionStatus.ACTIVE, saved.getStatus());
        assertTrue(saved.isActive());
        verify(promotionRepository).save(saved);
    }

    @Test
    void futureCampaignBecomesScheduled() {
        Promotion saved = promotionService.create(
                "Future Offer", null, PromotionType.SEASONAL_CAMPAIGN, PromotionStatus.ACTIVE, 1L,
                null, null, null, false,
                LocalDate.now().plusDays(2), LocalDate.now().plusDays(5), null, null);

        assertEquals(PromotionStatus.SCHEDULED, saved.getStatus());
        assertTrue(saved.isActive());
    }

    @Test
    void rejectsSoldLandForActivePromotion() {
        land.setStatus(LandStatus.SOLD);

        assertThrows(IllegalArgumentException.class, () -> promotionService.create(
                "Offer", null, PromotionType.SPECIAL_OFFER, PromotionStatus.ACTIVE, 1L,
                null, null, null, false, LocalDate.now(), LocalDate.now().plusDays(5), null, null));
    }

    @Test
    void rejectsUnverifiedLandForActivePromotion() {
        when(verificationService.hasApprovedVerification(1L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> promotionService.create(
                "Offer", null, PromotionType.SPECIAL_OFFER, PromotionStatus.ACTIVE, 1L,
                null, null, null, false, LocalDate.now(), LocalDate.now().plusDays(5), null, null));
    }

    @Test
    void expiredPromotionIsRemovedFromPublicResults() {
        Promotion expired = promotion(PromotionStatus.ACTIVE);
        expired.setStartDate(LocalDate.now().minusDays(5));
        expired.setEndDate(LocalDate.now().minusDays(1));
        expired.setActive(true);
        when(promotionRepository.findAll()).thenReturn(List.of(expired));
        when(promotionRepository.findByActiveTrueAndStatusOrderByStartDateAsc(PromotionStatus.ACTIVE)).thenReturn(List.of());

        assertTrue(promotionService.findPublicActive().isEmpty());
        assertEquals(PromotionStatus.EXPIRED, expired.getStatus());
    }

    @Test
    void cancellationSoftDeletesAndPreservesRecord() {
        Promotion existing = promotion(PromotionStatus.ACTIVE);
        existing.setId(7L);
        when(promotionRepository.findById(7L)).thenReturn(Optional.of(existing));

        Promotion cancelled = promotionService.cancel(7L, null);

        assertEquals(PromotionStatus.CANCELLED, cancelled.getStatus());
        assertFalse(cancelled.isActive());
        verify(promotionRepository).save(existing);
    }

    private Promotion promotion(PromotionStatus status) {
        Promotion promotion = new Promotion();
        promotion.setId(2L);
        promotion.setTitle("Offer");
        promotion.setPromotionType(PromotionType.SPECIAL_OFFER);
        promotion.setStatus(status);
        promotion.setLand(land);
        promotion.setStartDate(LocalDate.now());
        promotion.setEndDate(LocalDate.now().plusDays(5));
        promotion.setActive(true);
        return promotion;
    }
}
