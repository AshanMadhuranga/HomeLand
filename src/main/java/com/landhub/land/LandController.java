package com.landhub.land;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class LandController {

    private final LandService landService;

    public LandController(LandService landService) {
        this.landService = landService;
    }

    @GetMapping("/lands")
    public String lands(@RequestParam(required = false) String location,
                        @RequestParam(required = false) String type,
                        @RequestParam(required = false) String availability,
                        @RequestParam(required = false) BigDecimal minPrice,
                        @RequestParam(required = false) BigDecimal maxPrice,
                        @RequestParam(required = false) BigDecimal minSize,
                        @RequestParam(required = false) BigDecimal maxSize,
                        @RequestParam(required = false, defaultValue = "newest") String sortBy,
                        Model model) {
        List<Land> lands = landService.findPublicLands(location, type, availability, minPrice, maxPrice, minSize, maxSize, sortBy);

        model.addAttribute("lands", lands);
        model.addAttribute("resultCount", lands.size());
        model.addAttribute("landTypes", LandType.values());
        model.addAttribute("landStatuses", new LandStatus[]{LandStatus.AVAILABLE, LandStatus.RESERVED, LandStatus.SOLD, LandStatus.PENDING});
        model.addAttribute("selectedLocation", location);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedAvailability", availability);
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
                    model.addAttribute("land", land);
                    model.addAttribute("relatedLands", landService.findPublicLands(null, null, null, null, null, null, null, "newest")
                            .stream()
                            .filter(item -> !item.getId().equals(land.getId()))
                            .limit(3)
                            .toList());
                    return "land-details";
                })
                .orElseGet(() -> {
                    model.addAttribute("landNotFound", true);
                    model.addAttribute("relatedLands", landService.findPublicLands(null, null, null, null, null, null, null, "newest")
                            .stream()
                            .limit(3)
                            .toList());
                    return "land-details";
                });
    }
}
