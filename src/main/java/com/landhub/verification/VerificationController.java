package com.landhub.verification;

import com.landhub.auth.User;
import com.landhub.auth.UserService;
import com.landhub.land.Land;
import com.landhub.land.LandService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/verification")
public class VerificationController {

    private final VerificationService verificationService;
    private final LandService landService;
    private final UserService userService;

    public VerificationController(VerificationService verificationService,
                                  LandService landService,
                                  UserService userService) {
        this.verificationService = verificationService;
        this.landService = landService;
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) VerificationStatus status, Model model) {
        model.addAttribute("requests", verificationService.findAll(status));
        model.addAttribute("statuses", VerificationStatus.values());
        model.addAttribute("selectedStatus", status);
        return "verification/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return verificationService.findById(id)
                .map(request -> {
                    model.addAttribute("request", request);
                    model.addAttribute("documentTypes", VerificationDocumentType.values());
                    return "verification/details";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Verification request was not found.");
                    return "redirect:/verification";
                });
    }

    @GetMapping("/new/{landId}")
    public String createForm(@PathVariable Long landId, Model model, RedirectAttributes redirectAttributes) {
        Optional<Land> land = landService.findById(landId);

        if (land.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Land listing was not found.");
            return "redirect:/admin/lands";
        }

        model.addAttribute("land", land.get());
        model.addAttribute("documentTypes", VerificationDocumentType.values());
        return "verification/form";
    }

    @PostMapping
    public String create(@RequestParam Long landId,
                         @RequestParam String submittedByName,
                         @RequestParam(required = false) String notes,
                         @RequestParam(required = false) VerificationDocumentType documentType,
                         @RequestParam(value = "documents", required = false) MultipartFile[] documents,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        try {
            VerificationRequest request = verificationService.create(landId, submittedByName, notes, documentType, documents);
            redirectAttributes.addFlashAttribute("successMessage", "Verification request created successfully.");
            return "redirect:/verification/" + request.getId();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            landService.findById(landId).ifPresent(land -> model.addAttribute("land", land));
            model.addAttribute("submittedByName", submittedByName);
            model.addAttribute("notes", notes);
            model.addAttribute("documentTypes", VerificationDocumentType.values());
            return "verification/form";
        }
    }

    @PostMapping("/{id}/start-review")
    public String startReview(@PathVariable Long id,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            verificationService.startReview(id, currentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Verification request moved under review.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/verification/" + id;
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            verificationService.approve(id, currentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Verification approved. The linked land is now available publicly.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/verification/" + id;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam String rejectionReason,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            verificationService.reject(id, rejectionReason, currentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Verification request rejected.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/verification/" + id;
    }

    @PostMapping("/{id}/request-info")
    public String requestInfo(@PathVariable Long id,
                              @RequestParam String additionalInfoRequest,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            verificationService.requestAdditionalInfo(id, additionalInfoRequest, currentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Additional information requested.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/verification/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            verificationService.cancel(id);
            redirectAttributes.addFlashAttribute("successMessage", "Verification request cancelled.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/verification";
    }

    @PostMapping("/{id}/documents")
    public String uploadDocuments(@PathVariable Long id,
                                  @RequestParam(required = false) VerificationDocumentType documentType,
                                  @RequestParam(value = "documents", required = false) MultipartFile[] documents,
                                  RedirectAttributes redirectAttributes) {
        try {
            verificationService.uploadDocuments(id, documentType, documents);
            redirectAttributes.addFlashAttribute("successMessage", "Verification document uploaded successfully.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/verification/" + id;
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<Resource> viewDocument(@PathVariable Long documentId) {
        VerificationDocument document = verificationService.findActiveDocument(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document was not found."));
        Resource resource = verificationService.loadDocumentResource(document);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getOriginalFileName() + "\"")
                .body(resource);
    }

    @PostMapping("/{id}/documents/{documentId}/delete")
    public String deleteDocument(@PathVariable Long id,
                                 @PathVariable Long documentId,
                                 RedirectAttributes redirectAttributes) {
        try {
            verificationService.removeDocument(id, documentId);
            redirectAttributes.addFlashAttribute("successMessage", "Document removed from the active review file.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/verification/" + id;
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }

        return userService.findByEmail(authentication.getName()).orElse(null);
    }
}
