package com.landhub.marketing;

import com.landhub.auth.User;
import com.landhub.auth.UserService;
import com.landhub.land.LandService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/marketing")
public class MarketingController {

    private final PromotionService promotionService;
    private final LandService landService;
    private final UserService userService;

    public MarketingController(PromotionService promotionService,
                               LandService landService,
                               UserService userService) {
        this.promotionService = promotionService;
        this.landService = landService;
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) PromotionStatus status, Model model) {
        model.addAttribute("promotions", promotionService.findAll(status));
        model.addAttribute("statuses", PromotionStatus.values());
        model.addAttribute("selectedStatus", status);
        return "marketing/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        Promotion promotion = new Promotion();
        promotion.setPromotionType(PromotionType.SPECIAL_OFFER);
        promotion.setStatus(PromotionStatus.DRAFT);
        promotion.setStartDate(LocalDate.now());
        promotion.setEndDate(LocalDate.now().plusDays(30));
        addFormModel(model, promotion);
        return "marketing/form";
    }

    @PostMapping
    public String create(@RequestParam String title,
                         @RequestParam(required = false) String description,
                         @RequestParam PromotionType promotionType,
                         @RequestParam(defaultValue = "DRAFT") PromotionStatus status,
                         @RequestParam Long landId,
                         @RequestParam(required = false) BigDecimal discountPercentage,
                         @RequestParam(required = false) BigDecimal discountAmount,
                         @RequestParam(required = false) String promotionalText,
                         @RequestParam(defaultValue = "false") boolean featured,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                         @RequestParam(required = false) String bannerImageUrl,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Promotion form = formPromotion(title, description, promotionType, status, landId, discountPercentage,
                discountAmount, promotionalText, featured, startDate, endDate, bannerImageUrl);
        try {
            Promotion saved = promotionService.create(title, description, promotionType, status, landId,
                    discountPercentage, discountAmount, promotionalText, featured, startDate, endDate,
                    bannerImageUrl, currentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Promotion created successfully.");
            return "redirect:/marketing/" + saved.getId();
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            addFormModel(model, form);
            return "marketing/form";
        }
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return promotionService.findById(id)
                .map(promotion -> {
                    model.addAttribute("promotion", promotion);
                    return "marketing/details";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Promotion was not found.");
                    return "redirect:/marketing";
                });
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return promotionService.findById(id)
                .map(promotion -> {
                    addFormModel(model, promotion);
                    return "marketing/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Promotion was not found.");
                    return "redirect:/marketing";
                });
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @RequestParam String title,
                         @RequestParam(required = false) String description,
                         @RequestParam PromotionType promotionType,
                         @RequestParam(defaultValue = "DRAFT") PromotionStatus status,
                         @RequestParam Long landId,
                         @RequestParam(required = false) BigDecimal discountPercentage,
                         @RequestParam(required = false) BigDecimal discountAmount,
                         @RequestParam(required = false) String promotionalText,
                         @RequestParam(defaultValue = "false") boolean featured,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                         @RequestParam(required = false) String bannerImageUrl,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Promotion form = formPromotion(title, description, promotionType, status, landId, discountPercentage,
                discountAmount, promotionalText, featured, startDate, endDate, bannerImageUrl);
        form.setId(id);
        try {
            Promotion saved = promotionService.update(id, title, description, promotionType, status, landId,
                    discountPercentage, discountAmount, promotionalText, featured, startDate, endDate,
                    bannerImageUrl, currentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Promotion updated successfully.");
            return "redirect:/marketing/" + saved.getId();
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            addFormModel(model, form);
            return "marketing/form";
        }
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            promotionService.activate(id, currentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Promotion activated successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "Promotion cannot be activated because " + lowercaseFirst(exception.getMessage()));
        }
        return "redirect:/marketing/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        return changeStatus(() -> promotionService.cancel(id, currentUser(authentication)), "Promotion cancelled.", id, redirectAttributes);
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        return changeStatus(() -> promotionService.deactivate(id, currentUser(authentication)), "Promotion deactivated.", id, redirectAttributes);
    }

    private String changeStatus(StatusChange change, String success, Long id, RedirectAttributes redirectAttributes) {
        try {
            change.apply();
            redirectAttributes.addFlashAttribute("successMessage", success);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/marketing/" + id;
    }

    private void addFormModel(Model model, Promotion promotion) {
        model.addAttribute("promotion", promotion);
        model.addAttribute("lands", landService.findAllForAdmin());
        model.addAttribute("promotionTypes", PromotionType.values());
        model.addAttribute("promotionStatuses", new PromotionStatus[]{PromotionStatus.DRAFT, PromotionStatus.SCHEDULED, PromotionStatus.ACTIVE});
    }

    private Promotion formPromotion(String title, String description, PromotionType promotionType,
                                    PromotionStatus status, Long landId, BigDecimal discountPercentage,
                                    BigDecimal discountAmount, String promotionalText, boolean featured,
                                    LocalDate startDate, LocalDate endDate, String bannerImageUrl) {
        Promotion promotion = new Promotion();
        promotion.setTitle(title);
        promotion.setDescription(description);
        promotion.setPromotionType(promotionType);
        promotion.setStatus(status);
        promotion.setLand(landService.findById(landId).orElse(null));
        promotion.setDiscountPercentage(discountPercentage);
        promotion.setDiscountAmount(discountAmount);
        promotion.setPromotionalText(promotionalText);
        promotion.setFeatured(featured);
        promotion.setStartDate(startDate);
        promotion.setEndDate(endDate);
        promotion.setBannerImageUrl(bannerImageUrl);
        return promotion;
    }

    private User currentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated marketing account was not found."));
    }

    private String lowercaseFirst(String message) {
        if (message == null || message.isBlank()) {
            return "it does not meet the activation rules.";
        }
        return Character.toLowerCase(message.charAt(0)) + message.substring(1);
    }

    @FunctionalInterface
    private interface StatusChange {
        void apply();
    }
}
