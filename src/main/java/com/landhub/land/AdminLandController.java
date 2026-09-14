package com.landhub.land;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/lands")
public class AdminLandController {

    private final LandService landService;

    public AdminLandController(LandService landService) {
        this.landService = landService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("lands", landService.findAllForAdmin());
        return "admin/lands/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        Land land = new Land();
        land.setStatus(LandStatus.PENDING);
        land.setSizeUnit("Perches");
        model.addAttribute("land", land);
        addFormOptions(model);
        return "admin/lands/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("land") Land land,
                         BindingResult bindingResult,
                         @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormOptions(model);
            return "admin/lands/form";
        }

        try {
            Land savedLand = landService.create(land, imageFiles);
            redirectAttributes.addFlashAttribute("successMessage", "Land listing created successfully.");
            return "redirect:/admin/lands/" + savedLand.getId();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            bindingResult.reject("imageUpload", exception.getMessage());
            addFormOptions(model);
            return "admin/lands/form";
        }
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return landService.findById(id)
                .map(land -> {
                    model.addAttribute("land", land);
                    return "admin/lands/details";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Land listing was not found.");
                    return "redirect:/admin/lands";
                });
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return landService.findById(id)
                .map(land -> {
                    model.addAttribute("land", land);
                    addFormOptions(model);
                    return "admin/lands/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Land listing was not found.");
                    return "redirect:/admin/lands";
                });
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("land") Land land,
                         BindingResult bindingResult,
                         @RequestParam(value = "imageFiles", required = false) MultipartFile[] imageFiles,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            land.setId(id);
            addFormOptions(model);
            return "admin/lands/form";
        }

        try {
            Land savedLand = landService.update(id, land, imageFiles);
            redirectAttributes.addFlashAttribute("successMessage", "Land listing updated successfully.");
            return "redirect:/admin/lands/" + savedLand.getId();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            bindingResult.reject("landUpdate", exception.getMessage());
            land.setId(id);
            addFormOptions(model);
            return "admin/lands/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            landService.deactivate(id);
            redirectAttributes.addFlashAttribute("successMessage", "Land listing deactivated successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/admin/lands";
    }

    private void addFormOptions(Model model) {
        model.addAttribute("landTypes", LandType.values());
        model.addAttribute("landStatuses", LandStatus.values());
        model.addAttribute("sizeUnits", new String[]{"Perches", "Acres", "Square Feet"});
    }
}
