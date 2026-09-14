package com.landhub.booking;

import com.landhub.land.LandService;
import com.landhub.payment.PaymentService;
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
@RequestMapping("/customer/bookings")
public class CustomerBookingController {

    private final BookingService bookingService;
    private final LandService landService;
    private final PaymentService paymentService;

    public CustomerBookingController(BookingService bookingService, LandService landService, PaymentService paymentService) {
        this.bookingService = bookingService;
        this.landService = landService;
        this.paymentService = paymentService;
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        model.addAttribute("bookings", bookingService.findCustomerBookings(authentication.getName()));
        return "customer/bookings/list";
    }

    @GetMapping("/new/{landId}")
    public String form(@PathVariable Long landId, Model model, RedirectAttributes redirectAttributes) {
        return landService.findPublicLandById(landId)
                .map(land -> {
                    model.addAttribute("land", land);
                    return "customer/bookings/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Land listing was not found.");
                    return "redirect:/lands";
                });
    }

    @PostMapping
    public String create(@RequestParam Long landId,
                         @RequestParam(required = false) String customerNote,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingService.create(authentication.getName(), landId, customerNote);
            redirectAttributes.addFlashAttribute("successMessage", "Booking request submitted.");
            return "redirect:/customer/bookings/" + booking.getId();
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("customerNote", customerNote);
            landService.findById(landId).ifPresent(land -> model.addAttribute("land", land));
            return "customer/bookings/form";
        }
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id,
                          Authentication authentication,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        return bookingService.findCustomerBooking(id, authentication.getName())
                .map(booking -> {
                    model.addAttribute("booking", booking);
                    model.addAttribute("paymentSummary", paymentService.getSummary(booking));
                    return "customer/bookings/details";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Booking request was not found.");
                    return "redirect:/customer/bookings";
                });
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            bookingService.cancelCustomerBooking(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Booking request cancelled.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/customer/bookings/" + id;
    }
}
