package com.landhub.feedback;

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
@RequestMapping("/customer/feedback")
public class CustomerFeedbackController {

    private final FeedbackService feedbackService;

    public CustomerFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        model.addAttribute("feedbackItems", feedbackService.findCustomerFeedback(authentication.getName()));
        return "customer/feedback/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("types", FeedbackType.values());
        return "customer/feedback/form";
    }

    @PostMapping
    public String create(@RequestParam FeedbackType type,
                         @RequestParam String subject,
                         @RequestParam String message,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        try {
            Feedback feedback = feedbackService.create(authentication.getName(), type, subject, message);
            redirectAttributes.addFlashAttribute("successMessage", "Feedback submitted.");
            return "redirect:/customer/feedback/" + feedback.getId();
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("types", FeedbackType.values());
            model.addAttribute("type", type);
            model.addAttribute("subject", subject);
            model.addAttribute("message", message);
            return "customer/feedback/form";
        }
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        return feedbackService.findCustomerFeedback(id, authentication.getName())
                .map(feedback -> {
                    model.addAttribute("feedback", feedback);
                    return "customer/feedback/details";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Feedback was not found.");
                    return "redirect:/customer/feedback";
                });
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            feedbackService.cancel(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Feedback cancelled.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/customer/feedback/" + id;
    }
}
