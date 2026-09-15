package com.landhub.review;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/reviews")
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("reviews", reviewService.findAll());
        return "admin/reviews/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return reviewService.findById(id)
                .map(review -> {
                    model.addAttribute("review", review);
                    return "admin/reviews/details";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Review was not found.");
                    return "redirect:/admin/reviews";
                });
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reviewService.approve(id);
            redirectAttributes.addFlashAttribute("successMessage", "Review approved.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/reviews/" + id;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String reason, RedirectAttributes redirectAttributes) {
        try {
            reviewService.reject(id, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Review rejected.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/reviews/" + id;
    }

    @PostMapping("/{id}/hide")
    public String hide(@PathVariable Long id, @RequestParam(required = false) String reason, RedirectAttributes redirectAttributes) {
        try {
            reviewService.hide(id, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Review hidden.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/reviews/" + id;
    }
}
