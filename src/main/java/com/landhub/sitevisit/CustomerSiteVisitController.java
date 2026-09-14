package com.landhub.sitevisit;

import com.landhub.land.LandService;
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
@RequestMapping("/customer/site-visits")
public class CustomerSiteVisitController {

    private final SiteVisitService siteVisitService;
    private final LandService landService;

    public CustomerSiteVisitController(SiteVisitService siteVisitService, LandService landService) {
        this.siteVisitService = siteVisitService;
        this.landService = landService;
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        model.addAttribute("siteVisits", siteVisitService.findCustomerSiteVisits(authentication.getName()));
        return "customer/site-visits/list";
    }

    @GetMapping("/new/{landId}")
    public String form(@PathVariable Long landId, Model model, RedirectAttributes redirectAttributes) {
        return landService.findPublicLandById(landId)
                .map(land -> {
                    model.addAttribute("land", land);
                    return "customer/site-visits/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Land listing was not found.");
                    return "redirect:/lands";
                });
    }

    @PostMapping
    public String create(@RequestParam Long landId,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate preferredDate,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime preferredTime,
                         @RequestParam(required = false) String note,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        try {
            SiteVisit visit = siteVisitService.create(authentication.getName(), landId, preferredDate, preferredTime, note);
            redirectAttributes.addFlashAttribute("successMessage", "Site visit request submitted.");
            return "redirect:/customer/site-visits/" + visit.getId();
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("preferredDate", preferredDate);
            model.addAttribute("preferredTime", preferredTime);
            model.addAttribute("note", note);
            landService.findById(landId).ifPresent(land -> model.addAttribute("land", land));
            return "customer/site-visits/form";
        }
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id,
                          Authentication authentication,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        return siteVisitService.findCustomerSiteVisit(id, authentication.getName())
                .map(visit -> {
                    model.addAttribute("siteVisit", visit);
                    return "customer/site-visits/details";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Site visit request was not found.");
                    return "redirect:/customer/site-visits";
                });
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            siteVisitService.cancelCustomerSiteVisit(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Site visit request cancelled.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/customer/site-visits/" + id;
    }
}
