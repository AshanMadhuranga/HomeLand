package com.landhub.marketing;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PublicPromotionController {

    private final PromotionService promotionService;

    public PublicPromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping("/promotions")
    public String list(Model model) {
        model.addAttribute("promotions", promotionService.findPublicActive());
        return "promotions/list";
    }

    @GetMapping("/promotions/{id}")
    public String details(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return promotionService.viewPublicPromotion(id)
                .map(promotion -> {
                    model.addAttribute("promotion", promotion);
                    return "promotions/details";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Promotion was not found or is no longer active.");
                    return "redirect:/promotions";
                });
    }
}
