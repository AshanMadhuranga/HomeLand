package com.landhub.marketing;

import com.landhub.auth.User;
import com.landhub.land.Land;
import com.landhub.land.LandService;
import com.landhub.land.LandStatus;
import com.landhub.verification.VerificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final LandService landService;
    private final VerificationService verificationService;

    public PromotionService(PromotionRepository promotionRepository,
                            LandService landService,
                            VerificationService verificationService) {
        this.promotionRepository = promotionRepository;
        this.landService = landService;
        this.verificationService = verificationService;
    }

    public List<Promotion> findAll(PromotionStatus status) {
        synchronizeLifecycle();
        return status == null
                ? promotionRepository.findAllByOrderByCreatedAtDesc()
                : promotionRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public List<Promotion> findPublicActive() {
        synchronizeLifecycle();
        return promotionRepository.findByActiveTrueAndStatusOrderByStartDateAsc(PromotionStatus.ACTIVE)
                .stream()
                .filter(this::isPubliclyEligible)
                .toList();
    }

    public Map<Long, Promotion> publicPromotionsByLand(Collection<Land> lands) {
        return findPublicActive().stream()
                .filter(promotion -> lands.stream().anyMatch(land -> land.getId().equals(promotion.getLand().getId())))
                .collect(Collectors.toMap(promotion -> promotion.getLand().getId(), Function.identity(), (first, second) -> first));
    }

    @Transactional(readOnly = true)
    public Optional<Promotion> findPublicActiveForLand(Long landId) {
        return findPublicActive().stream()
                .filter(promotion -> promotion.getLand().getId().equals(landId))
                .findFirst();
    }

    @Transactional(readOnly = true)
    public Optional<Promotion> findById(Long id) {
        return promotionRepository.findById(id);
    }

    public Optional<Promotion> viewPublicPromotion(Long id) {
        synchronizeLifecycle();
        return promotionRepository.findById(id)
                .filter(this::isPubliclyEligible)
                .map(promotion -> {
                    promotion.setViewCount(promotion.getViewCount() + 1);
                    return promotion;
                });
    }

    public Promotion create(String title,
                            String description,
                            PromotionType promotionType,
                            PromotionStatus requestedStatus,
                            Long landId,
                            BigDecimal discountPercentage,
                            BigDecimal discountAmount,
                            String promotionalText,
                            boolean featured,
                            LocalDate startDate,
                            LocalDate endDate,
                            String bannerImageUrl,
                            User actor) {
        Promotion promotion = new Promotion();
        promotion.setTitle(title);
        promotion.setDescription(description);
        promotion.setPromotionType(promotionType);
        promotion.setLand(findLand(landId));
        promotion.setDiscountPercentage(discountPercentage);
        promotion.setDiscountAmount(discountAmount);
        promotion.setPromotionalText(promotionalText);
        promotion.setFeatured(featured);
        promotion.setStartDate(startDate);
        promotion.setEndDate(endDate);
        promotion.setBannerImageUrl(bannerImageUrl);
        promotion.setCreatedBy(actor);
        promotion.setUpdatedBy(actor);
        applyStatus(promotion, requestedStatus == null ? PromotionStatus.DRAFT : requestedStatus);
        validate(promotion);
        return saveWithFeaturedRule(promotion);
    }

    public Promotion update(Long id,
                            String title,
                            String description,
                            PromotionType promotionType,
                            PromotionStatus requestedStatus,
                            Long landId,
                            BigDecimal discountPercentage,
                            BigDecimal discountAmount,
                            String promotionalText,
                            boolean featured,
                            LocalDate startDate,
                            LocalDate endDate,
                            String bannerImageUrl,
                            User actor) {
        Promotion promotion = getPromotion(id);
        promotion.setTitle(title);
        promotion.setDescription(description);
        promotion.setPromotionType(promotionType);
        promotion.setLand(findLand(landId));
        promotion.setDiscountPercentage(discountPercentage);
        promotion.setDiscountAmount(discountAmount);
        promotion.setPromotionalText(promotionalText);
        promotion.setFeatured(featured);
        promotion.setStartDate(startDate);
        promotion.setEndDate(endDate);
        promotion.setBannerImageUrl(bannerImageUrl);
        promotion.setUpdatedBy(actor);
        if (promotion.getStatus() != PromotionStatus.CANCELLED
                && promotion.getStatus() != PromotionStatus.INACTIVE) {
            applyStatus(promotion, requestedStatus == null ? promotion.getStatus() : requestedStatus);
        }
        validate(promotion);
        return saveWithFeaturedRule(promotion);
    }

    public Promotion activate(Long id, User actor) {
        Promotion promotion = getPromotion(id);
        if (promotion.getStatus() == PromotionStatus.CANCELLED
                || promotion.getStatus() == PromotionStatus.INACTIVE) {
            throw new IllegalArgumentException("Cancelled or inactive promotions cannot be activated.");
        }
        if (promotion.getStartDate() == null || promotion.getEndDate() == null) {
            throw new IllegalArgumentException("Promotion start and end dates are required.");
        }
        promotion.setActive(true);
        promotion.setUpdatedBy(actor);
        applyStatus(promotion, lifecycleStatus(promotion.getStartDate(), promotion.getEndDate()));
        validate(promotion);
        return saveWithFeaturedRule(promotion);
    }

    public Promotion cancel(Long id, User actor) {
        Promotion promotion = getPromotion(id);
        if (promotion.getStatus() == PromotionStatus.CANCELLED) {
            throw new IllegalArgumentException("Promotion is already cancelled.");
        }
        promotion.setStatus(PromotionStatus.CANCELLED);
        promotion.setActive(false);
        promotion.setUpdatedBy(actor);
        return promotionRepository.save(promotion);
    }

    public Promotion deactivate(Long id, User actor) {
        Promotion promotion = getPromotion(id);
        promotion.setStatus(PromotionStatus.INACTIVE);
        promotion.setActive(false);
        promotion.setUpdatedBy(actor);
        return promotionRepository.save(promotion);
    }

    public long countActive() {
        synchronizeLifecycle();
        return promotionRepository.countByActiveTrueAndStatus(PromotionStatus.ACTIVE);
    }

    public long countScheduled() {
        synchronizeLifecycle();
        return promotionRepository.countByActiveTrueAndStatus(PromotionStatus.SCHEDULED);
    }

    private void synchronizeLifecycle() {
        LocalDate today = LocalDate.now();
        promotionRepository.findAll().stream()
                .filter(Promotion::isActive)
                .filter(promotion -> promotion.getStatus() == PromotionStatus.SCHEDULED
                        || promotion.getStatus() == PromotionStatus.ACTIVE)
                .forEach(promotion -> {
                    PromotionStatus status = lifecycleStatus(promotion.getStartDate(), promotion.getEndDate(), today);
                    if (promotion.getStatus() != status) {
                        promotion.setStatus(status);
                        promotionRepository.save(promotion);
                    }
                });
    }

    private void applyStatus(Promotion promotion, PromotionStatus requestedStatus) {
        if (requestedStatus == PromotionStatus.DRAFT) {
            promotion.setStatus(PromotionStatus.DRAFT);
            promotion.setActive(false);
            return;
        }
        promotion.setActive(true);
        promotion.setStatus(requestedStatus == PromotionStatus.CANCELLED || requestedStatus == PromotionStatus.INACTIVE
                ? requestedStatus
            : promotion.getStartDate() == null || promotion.getEndDate() == null
            ? requestedStatus
            : lifecycleStatus(promotion.getStartDate(), promotion.getEndDate()));
    }

    private PromotionStatus lifecycleStatus(LocalDate startDate, LocalDate endDate) {
        return lifecycleStatus(startDate, endDate, LocalDate.now());
    }

    private PromotionStatus lifecycleStatus(LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (today.isBefore(startDate)) {
            return PromotionStatus.SCHEDULED;
        }
        if (today.isAfter(endDate)) {
            return PromotionStatus.EXPIRED;
        }
        return PromotionStatus.ACTIVE;
    }

    private void validate(Promotion promotion) {
        if (isBlank(promotion.getTitle())) {
            throw new IllegalArgumentException("Promotion title is required.");
        }
        if (promotion.getLand() == null) {
            throw new IllegalArgumentException("An existing land listing is required.");
        }
        if (promotion.getPromotionType() == null) {
            throw new IllegalArgumentException("Promotion type is required.");
        }
        if (promotion.getStartDate() == null || promotion.getEndDate() == null) {
            throw new IllegalArgumentException("Promotion start and end dates are required.");
        }
        if (promotion.getEndDate().isBefore(promotion.getStartDate())) {
            throw new IllegalArgumentException("Promotion end date cannot be before its start date.");
        }
        if (promotion.getDiscountPercentage() != null && promotion.getDiscountAmount() != null) {
            throw new IllegalArgumentException("Use either a percentage discount or a fixed discount amount, not both.");
        }
        if (promotion.getPromotionType() == PromotionType.DISCOUNT
                && promotion.getDiscountPercentage() == null
                && promotion.getDiscountAmount() == null) {
            throw new IllegalArgumentException("A discount promotion requires a percentage or fixed amount.");
        }
        if (promotion.getDiscountPercentage() != null
                && (promotion.getDiscountPercentage().compareTo(BigDecimal.ZERO) <= 0
                || promotion.getDiscountPercentage().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException("Discount percentage must be greater than zero and no more than 100.");
        }
        if (promotion.getDiscountAmount() != null
                && promotion.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Discount amount must be greater than zero.");
        }
        if (promotion.isActive()) {
            validatePublicLand(promotion.getLand());
            if (!verificationService.hasApprovedVerification(promotion.getLand().getId())) {
                throw new IllegalArgumentException("Only verified land can receive an active public promotion.");
            }
        }
    }

    private void validatePublicLand(Land land) {
        if (land.getStatus() != LandStatus.AVAILABLE && land.getStatus() != LandStatus.RESERVED) {
            throw new IllegalArgumentException("Only available or reserved land can receive a public promotion.");
        }
    }

    private Promotion saveWithFeaturedRule(Promotion promotion) {
        if (promotion.isFeatured() && promotion.isActive()) {
            promotionRepository.findAll().stream()
                    .filter(existing -> !existing.getId().equals(promotion.getId()))
                    .filter(existing -> existing.isActive() && existing.isFeatured())
                    .filter(existing -> existing.getLand().getId().equals(promotion.getLand().getId()))
                    .forEach(existing -> {
                        existing.setFeatured(false);
                        promotionRepository.save(existing);
                    });
        }
        return promotionRepository.save(promotion);
    }

    private boolean isPubliclyEligible(Promotion promotion) {
        return promotion.isActive()
                && promotion.getStatus() == PromotionStatus.ACTIVE
                && promotion.getLand() != null
                && (promotion.getLand().getStatus() == LandStatus.AVAILABLE
                || promotion.getLand().getStatus() == LandStatus.RESERVED)
                && !LocalDate.now().isBefore(promotion.getStartDate())
                && !LocalDate.now().isAfter(promotion.getEndDate());
    }

    private Land findLand(Long landId) {
        if (landId == null) {
            throw new IllegalArgumentException("Land listing is required.");
        }
        return landService.findById(landId)
                .orElseThrow(() -> new IllegalArgumentException("Land listing was not found."));
    }

    private Promotion getPromotion(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion was not found."));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
