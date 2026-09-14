package com.landhub.sitevisit;

import com.landhub.auth.User;
import com.landhub.auth.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;

@Controller
@RequestMapping("/admin/site-visits")
public class AdminSiteVisitController {

    private final SiteVisitService siteVisitService;
    private final UserService userService;

    public AdminSiteVisitController(SiteVisitService siteVisitService, UserService userService) {
        this.siteVisitService = siteVisitService;
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("siteVisits", siteVisitService.findAll());
        return "admin/site-visits/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return siteVisitService.findById(id)
                .map(visit -> {
                    model.addAttribute("siteVisit", visit);
                    return "admin/site-visits/details";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Site visit request was not found.");
                    return "redirect:/admin/site-visits";
                });
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(required = false) String adminNote,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            siteVisitService.approve(id, adminNote, currentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Site visit approved.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/site-visits/" + id;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam String adminNote,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            siteVisitService.reject(id, adminNote, currentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Site visit rejected.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/site-visits/" + id;
    }

    @PostMapping("/{id}/reschedule")
    public String reschedule(@PathVariable Long id,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate preferredDate,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime preferredTime,
                             @RequestParam String adminNote,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            siteVisitService.reschedule(id, preferredDate, preferredTime, adminNote, currentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Site visit rescheduled.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/site-visits/" + id;
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        try {
            siteVisitService.complete(id, currentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Site visit marked complete.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/site-visits/" + id;
    }

    private User currentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName()).orElse(null);
    }
}
