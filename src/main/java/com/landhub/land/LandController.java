package com.landhub.land;

import com.landhub.marketing.PromotionService;
import com.landhub.verification.VerificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Controller
public class LandController {

    private final LandService landService;
    private final VerificationService verificationService;
    private final PromotionService promotionService;

    public LandController(LandService landService, VerificationService verificationService, PromotionService promotionService) {
        this.landService = landService;
        this.verificationService = verificationService;
        this.promotionService = promotionService;
    }

    @GetMapping("/lands")
    public String lands(@RequestParam(required = false) String location,
                        @RequestParam(required = false) String type,
                        @RequestParam(required = false) String availability,
                        @RequestParam(required = false) BigDecimal minPrice,
                        @RequestParam(required = false) BigDecimal maxPrice,
                        @RequestParam(required = false) BigDecimal minSize,
                        @RequestParam(required = false) BigDecimal maxSize,
                        @RequestParam(required = false) String verifiedOnly,
                        @RequestParam(required = false) String verifiedOnlyTouched,
                        @RequestParam(required = false, defaultValue = "newest") String sortBy,
                        Model model) {
        List<Land> lands = landService.findPublicLands(location, type, availability, minPrice, maxPrice, minSize, maxSize, sortBy);
        Set<Long> verifiedLandIds = verificationService.findApprovedLandIds(lands);
        boolean onlyVerified = verifiedOnlyTouched == null || "true".equalsIgnoreCase(verifiedOnly);

        if (onlyVerified) {
            lands = lands.stream()
                    .filter(land -> verifiedLandIds.contains(land.getId()))
                    .toList();
        }

        model.addAttribute("lands", lands);
        model.addAttribute("promotionsByLand", promotionService.publicPromotionsByLand(lands));
        model.addAttribute("verifiedLandIds", verifiedLandIds);
        model.addAttribute("resultCount", lands.size());
        model.addAttribute("landTypes", LandType.values());
        model.addAttribute("landStatuses", new LandStatus[]{LandStatus.AVAILABLE, LandStatus.RESERVED, LandStatus.SOLD});
        model.addAttribute("selectedLocation", location);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedAvailability", availability);
        model.addAttribute("selectedVerifiedOnly", onlyVerified);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("minSize", minSize);
        model.addAttribute("maxSize", maxSize);
        model.addAttribute("sortBy", sortBy);
        return "lands";
    }

    @GetMapping("/lands/{id}")
    public String landDetails(@PathVariable Long id, Model model) {
        return landService.findPublicLandById(id)
                .map(land -> {
                    List<Land> relatedLands = landService.findPublicLands(null, null, null, null, null, null, null, "newest")
                            .stream()
                            .filter(item -> !item.getId().equals(land.getId()))
                            .limit(3)
                            .toList();
                    model.addAttribute("land", land);
                    model.addAttribute("promotion", promotionService.findPublicActiveForLand(land.getId()).orElse(null));
                    model.addAttribute("verified", verificationService.hasApprovedVerification(land.getId()));
                    model.addAttribute("relatedLands", relatedLands);
                    model.addAttribute("verifiedLandIds", verificationService.findApprovedLandIds(relatedLands));
                    model.addAttribute("promotionsByLand", promotionService.publicPromotionsByLand(relatedLands));
                    return "land-details";
                })
                .orElseGet(() -> {
                    List<Land> relatedLands = landService.findPublicLands(null, null, null, null, null, null, null, "newest")
                            .stream()
                            .limit(3)
                            .toList();
                    model.addAttribute("landNotFound", true);
                    model.addAttribute("relatedLands", relatedLands);
                    model.addAttribute("verifiedLandIds", verificationService.findApprovedLandIds(relatedLands));
                    model.addAttribute("promotionsByLand", promotionService.publicPromotionsByLand(relatedLands));
                    return "land-details";
                });
    }
}
