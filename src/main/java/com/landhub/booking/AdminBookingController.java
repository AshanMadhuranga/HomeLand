package com.landhub.booking;

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

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/bookings")
public class AdminBookingController {

    private final BookingService bookingService;
    private final UserService userService;

    public AdminBookingController(BookingService bookingService, UserService userService) {
        this.bookingService = bookingService;
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("bookings", bookingService.findAll());
        return "admin/bookings/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return bookingService.findById(id)
                .map(booking -> {
                    model.addAttribute("booking", booking);
                    return "admin/bookings/details";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Booking request was not found.");
                    return "redirect:/admin/bookings";
                });
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(required = false) BigDecimal agreedPrice,
                          @RequestParam(required = false) String adminNote,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            bookingService.approve(id, agreedPrice, adminNote, currentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Booking approved and land reserved.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/bookings/" + id;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam String rejectionReason,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            bookingService.reject(id, rejectionReason, currentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Booking rejected.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/bookings/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         @RequestParam(required = false) String adminNote,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            bookingService.cancelByAdmin(id, adminNote, currentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Booking cancelled.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/bookings/" + id;
    }

    private User currentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName()).orElse(null);
    }
}
