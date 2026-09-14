package com.landhub.payment;

import com.landhub.booking.Booking;
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
@RequestMapping("/customer")
public class CustomerPaymentController {

    private final PaymentService paymentService;

    public CustomerPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/payments")
    public String list(Authentication authentication, Model model) {
        model.addAttribute("payments", paymentService.findCustomerPayments(authentication.getName()));
        return "customer/payments/list";
    }

    @GetMapping("/bookings/{bookingId}/payments")
    public String bookingPayments(@PathVariable Long bookingId,
                                  Authentication authentication,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        try {
            PaymentSummary summary = paymentService.getCustomerBookingSummary(bookingId, authentication.getName());
            model.addAttribute("summary", summary);
            model.addAttribute("payments", paymentService.findCustomerBookingPayments(bookingId, authentication.getName()));
            return "customer/payments/booking-summary";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/customer/bookings";
        }
    }

    @GetMapping("/payments/new/{bookingId}")
    public String form(@PathVariable Long bookingId,
                       Authentication authentication,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        try {
            addFormModel(model, paymentService.getCustomerBookingSummary(bookingId, authentication.getName()), null);
            return "customer/payments/form";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/customer/bookings/" + bookingId;
        }
    }

    @PostMapping("/payments")
    public String create(@RequestParam Long bookingId,
                         @RequestParam PaymentType paymentType,
                         @RequestParam PaymentMethod paymentMethod,
                         @RequestParam BigDecimal amount,
                         @RequestParam(required = false) String bankReference,
                         @RequestParam(required = false) String chequeNumber,
                         @RequestParam(required = false) String note,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        try {
            Payment payment = paymentService.createCustomerPayment(
                    authentication.getName(), bookingId, paymentType, paymentMethod, amount,
                    bankReference, chequeNumber, note);
            redirectAttributes.addFlashAttribute("successMessage", "Payment record created.");
            if (payment.getPaymentMethod() == PaymentMethod.DEMO_CARD_GATEWAY) {
                return "redirect:/customer/payments/" + payment.getId() + "/demo-gateway";
            }
            return "redirect:/customer/payments/" + payment.getId();
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("selectedPaymentType", paymentType);
            model.addAttribute("selectedPaymentMethod", paymentMethod);
            model.addAttribute("amount", amount);
            model.addAttribute("bankReference", bankReference);
            model.addAttribute("chequeNumber", chequeNumber);
            model.addAttribute("note", note);
            try {
                addFormModel(model, paymentService.getCustomerBookingSummary(bookingId, authentication.getName()), null);
            } catch (IllegalArgumentException ignored) {
                Booking emptyBooking = new Booking();
                emptyBooking.setId(bookingId);
                model.addAttribute("summary", new PaymentSummary(emptyBooking, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
            }
            return "customer/payments/form";
        }
    }

    @GetMapping("/payments/{id}")
    public String details(@PathVariable Long id,
                          Authentication authentication,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        return paymentService.findCustomerPayment(id, authentication.getName())
                .map(payment -> {
                    model.addAttribute("payment", payment);
                    model.addAttribute("summary", paymentService.getSummary(payment.getBooking()));
                    return "customer/payments/details";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Payment was not found.");
                    return "redirect:/customer/payments";
                });
    }

    @GetMapping("/payments/{id}/receipt")
    public String receipt(@PathVariable Long id,
                          Authentication authentication,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        return paymentService.findCustomerPayment(id, authentication.getName())
                .map(payment -> {
                    if (payment.getStatus() != PaymentStatus.PAID) {
                        redirectAttributes.addFlashAttribute("errorMessage", "Receipt is available only for paid payments.");
                        return "redirect:/customer/payments/" + id;
                    }
                    model.addAttribute("payment", payment);
                    return "customer/payments/receipt";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Payment was not found.");
                    return "redirect:/customer/payments";
                });
    }

    @GetMapping("/payments/{id}/demo-gateway")
    public String demoGateway(@PathVariable Long id,
                              Authentication authentication,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        return paymentService.findCustomerPayment(id, authentication.getName())
                .map(payment -> {
                    if (payment.getPaymentMethod() != PaymentMethod.DEMO_CARD_GATEWAY) {
                        redirectAttributes.addFlashAttribute("errorMessage", "This payment does not use the demo gateway.");
                        return "redirect:/customer/payments/" + id;
                    }
                    model.addAttribute("payment", payment);
                    return "customer/payments/demo-gateway";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Payment was not found.");
                    return "redirect:/customer/payments";
                });
    }

    @PostMapping("/payments/{id}/demo-success")
    public String demoSuccess(@PathVariable Long id,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            paymentService.simulateDemoSuccess(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Demo payment marked as paid.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/customer/payments/" + id;
    }

    @PostMapping("/payments/{id}/demo-failure")
    public String demoFailure(@PathVariable Long id,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            paymentService.simulateDemoFailure(id, authentication.getName());
            redirectAttributes.addFlashAttribute("errorMessage", "Demo payment failed.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/customer/payments/" + id;
    }

    private void addFormModel(Model model, PaymentSummary summary, Payment payment) {
        model.addAttribute("summary", summary);
        model.addAttribute("payment", payment);
        model.addAttribute("paymentTypes", PaymentType.values());
        model.addAttribute("paymentMethods", PaymentMethod.values());
    }
}
