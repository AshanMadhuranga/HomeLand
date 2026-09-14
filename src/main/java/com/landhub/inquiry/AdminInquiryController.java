package com.landhub.inquiry;

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
@RequestMapping("/admin/inquiries")
public class AdminInquiryController {

    private final InquiryService inquiryService;
    private final UserService userService;

    public AdminInquiryController(InquiryService inquiryService, UserService userService) {
        this.inquiryService = inquiryService;
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("inquiries", inquiryService.findAll());
        return "admin/inquiries/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return inquiryService.findById(id)
                .map(inquiry -> {
                    model.addAttribute("inquiry", inquiry);
                    return "admin/inquiries/details";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Inquiry was not found.");
                    return "redirect:/admin/inquiries";
                });
    }

    @PostMapping("/{id}/respond")
    public String respond(@PathVariable Long id,
                          @RequestParam String response,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            inquiryService.respond(id, response, currentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Inquiry response sent.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/inquiries/" + id;
    }

    @PostMapping("/{id}/resolve")
    public String resolve(@PathVariable Long id,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            inquiryService.resolve(id, currentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Inquiry marked resolved.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/inquiries/" + id;
    }

    private User currentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName()).orElse(null);
    }
}
