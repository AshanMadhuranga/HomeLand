package com.landhub.inquiry;

import com.landhub.land.LandService;
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
@RequestMapping("/customer/inquiries")
public class CustomerInquiryController {

    private final InquiryService inquiryService;
    private final LandService landService;

    public CustomerInquiryController(InquiryService inquiryService, LandService landService) {
        this.inquiryService = inquiryService;
        this.landService = landService;
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        model.addAttribute("inquiries", inquiryService.findCustomerInquiries(authentication.getName()));
        return "customer/inquiries/list";
    }

    @GetMapping("/new/{landId}")
    public String form(@PathVariable Long landId, Model model, RedirectAttributes redirectAttributes) {
        return landService.findPublicLandById(landId)
                .map(land -> {
                    model.addAttribute("land", land);
                    return "customer/inquiries/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Land listing was not found.");
                    return "redirect:/lands";
                });
    }

    @PostMapping
    public String create(@RequestParam Long landId,
                         @RequestParam String subject,
                         @RequestParam String message,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        try {
            Inquiry inquiry = inquiryService.create(authentication.getName(), landId, subject, message);
            redirectAttributes.addFlashAttribute("successMessage", "Inquiry sent successfully.");
            return "redirect:/customer/inquiries/" + inquiry.getId();
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("subject", subject);
            model.addAttribute("message", message);
            landService.findById(landId).ifPresent(land -> model.addAttribute("land", land));
            return "customer/inquiries/form";
        }
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id,
                          Authentication authentication,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        return inquiryService.findCustomerInquiry(id, authentication.getName())
                .map(inquiry -> {
                    model.addAttribute("inquiry", inquiry);
                    return "customer/inquiries/details";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Inquiry was not found.");
                    return "redirect:/customer/inquiries";
                });
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            inquiryService.cancelCustomerInquiry(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Inquiry cancelled.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/customer/inquiries/" + id;
    }
}
