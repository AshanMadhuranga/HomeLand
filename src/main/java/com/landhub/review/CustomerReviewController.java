package com.landhub.review;

import com.landhub.booking.BookingService;
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
@RequestMapping("/customer/reviews")
public class CustomerReviewController {

    private final ReviewService reviewService;
    private final BookingService bookingService;

    public CustomerReviewController(ReviewService reviewService, BookingService bookingService) {
        this.reviewService = reviewService;
        this.bookingService = bookingService;
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        model.addAttribute("reviews", reviewService.findCustomerReviews(authentication.getName()));
        return "customer/reviews/list";
    }

    @GetMapping("/new/{bookingId}")
    public String form(@PathVariable Long bookingId, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        return bookingService.findCustomerBooking(bookingId, authentication.getName())
                .map(booking -> {
                    model.addAttribute("booking", booking);
                    model.addAttribute("review", null);
                    return "customer/reviews/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Completed booking was not found.");
                    return "redirect:/customer/bookings";
                });
    }

    @PostMapping
    public String create(@RequestParam Long bookingId,
                         @RequestParam Integer rating,
                         @RequestParam String title,
                         @RequestParam String comment,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        try {
            Review review = reviewService.create(authentication.getName(), bookingId, rating, title, comment);
            redirectAttributes.addFlashAttribute("successMessage", "Review submitted for moderation.");
            return "redirect:/customer/reviews/" + review.getId();
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            bookingService.findCustomerBooking(bookingId, authentication.getName()).ifPresent(booking -> model.addAttribute("booking", booking));
            model.addAttribute("rating", rating);
            model.addAttribute("title", title);
            model.addAttribute("comment", comment);
            return "customer/reviews/form";
        }
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        return reviewService.findCustomerReview(id, authentication.getName())
                .map(review -> {
                    model.addAttribute("review", review);
                    return "customer/reviews/details";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Review was not found.");
                    return "redirect:/customer/reviews";
                });
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        return reviewService.findCustomerReview(id, authentication.getName())
                .map(review -> {
                    model.addAttribute("review", review);
                    model.addAttribute("booking", review.getBooking());
                    return "customer/reviews/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Review was not found.");
                    return "redirect:/customer/reviews";
                });
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @RequestParam Integer rating,
                         @RequestParam String title,
                         @RequestParam String comment,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            reviewService.update(id, authentication.getName(), rating, title, comment);
            redirectAttributes.addFlashAttribute("successMessage", "Review updated and sent for moderation.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/customer/reviews/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            reviewService.hideByCustomer(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Review hidden.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/customer/reviews";
    }
}
