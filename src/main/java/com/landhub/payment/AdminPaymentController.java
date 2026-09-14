package com.landhub.payment;

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

import java.util.Optional;

@Controller
@RequestMapping("/admin/payments")
public class AdminPaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    public AdminPaymentController(PaymentService paymentService, UserService userService) {
        this.paymentService = paymentService;
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) PaymentStatus status, Model model) {
        model.addAttribute("payments", paymentService.findAll(Optional.ofNullable(status)));
        model.addAttribute("statuses", PaymentStatus.values());
        model.addAttribute("selectedStatus", status);
        return "admin/payments/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return paymentService.findById(id)
                .map(payment -> {
                    model.addAttribute("payment", payment);
                    model.addAttribute("summary", paymentService.getSummary(payment.getBooking()));
                    return "admin/payments/details";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Payment was not found.");
                    return "redirect:/admin/payments";
                });
    }

    @PostMapping("/{id}/mark-paid")
    public String markPaid(@PathVariable Long id,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        try {
            paymentService.markPaidByAdmin(id, currentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Payment marked as paid.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/payments/" + id;
    }

    @PostMapping("/{id}/mark-failed")
    public String markFailed(@PathVariable Long id,
                             @RequestParam String failureReason,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            paymentService.markFailedByAdmin(id, failureReason, currentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Payment marked as failed.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/payments/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            paymentService.cancel(id, currentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Payment cancelled.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/payments/" + id;
    }

    @PostMapping("/{id}/refund")
    public String refund(@PathVariable Long id,
                         @RequestParam String refundReason,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            paymentService.refund(id, refundReason, currentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Payment refunded.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/payments/" + id;
    }

    private User currentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated staff account was not found."));
    }
}
