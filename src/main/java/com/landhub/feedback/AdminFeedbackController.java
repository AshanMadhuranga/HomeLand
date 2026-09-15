package com.landhub.feedback;

import com.landhub.auth.User;
import com.landhub.auth.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/feedback")
public class AdminFeedbackController {

    private final FeedbackService feedbackService;
    private final UserService userService;

    public AdminFeedbackController(FeedbackService feedbackService, UserService userService) {
        this.feedbackService = feedbackService;
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("feedbackItems", feedbackService.findAll());
        return "admin/feedback/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return feedbackService.findById(id)
                .map(feedback -> {
                    model.addAttribute("feedback", feedback);
                    return "admin/feedback/details";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Feedback was not found.");
                    return "redirect:/admin/feedback";
                });
    }

    @PostMapping("/{id}/review")
    public String review(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            feedbackService.markInReview(id);
            redirectAttributes.addFlashAttribute("successMessage", "Feedback marked in review.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/feedback/" + id;
    }

    @PostMapping("/{id}/respond")
    public String respond(@PathVariable Long id,
                          @RequestParam String adminResponse,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            feedbackService.respond(id, adminResponse, currentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Response sent.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/feedback/" + id;
    }

    @PostMapping("/{id}/resolve")
    public String resolve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            feedbackService.resolve(id);
            redirectAttributes.addFlashAttribute("successMessage", "Feedback resolved.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/feedback/" + id;
    }

    private User currentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName()).orElse(null);
    }
}
