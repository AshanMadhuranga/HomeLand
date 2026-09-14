package com.landhub.verification;

import com.landhub.auth.User;
import com.landhub.land.Land;
import com.landhub.land.LandService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class VerificationService {

    private static final long MAX_DOCUMENT_SIZE = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Set<VerificationStatus> ACTIVE_REVIEW_STATUSES = Set.of(
            VerificationStatus.PENDING,
            VerificationStatus.UNDER_REVIEW,
            VerificationStatus.ADDITIONAL_INFO_REQUIRED
    );

    private final VerificationRequestRepository requestRepository;
    private final VerificationDocumentRepository documentRepository;
    private final LandService landService;
    private final Path uploadRoot = Paths.get("uploads", "verifications").toAbsolutePath().normalize();

    public VerificationService(VerificationRequestRepository requestRepository,
                               VerificationDocumentRepository documentRepository,
                               LandService landService) {
        this.requestRepository = requestRepository;
        this.documentRepository = documentRepository;
        this.landService = landService;
    }

    @Transactional(readOnly = true)
    public List<VerificationRequest> findAll(VerificationStatus status) {
        if (status == null) {
            return requestRepository.findAllByActiveTrueOrderBySubmittedAtDesc();
        }

        return requestRepository.findByStatusAndActiveTrueOrderBySubmittedAtDesc(status);
    }

    @Transactional(readOnly = true)
    public Optional<VerificationRequest> findById(Long id) {
        return requestRepository.findById(id).filter(VerificationRequest::isActive);
    }

    @Transactional(readOnly = true)
    public Optional<VerificationDocument> findActiveDocument(Long documentId) {
        return documentRepository.findById(documentId).filter(VerificationDocument::isActive);
    }

    @Transactional(readOnly = true)
    public Resource loadDocumentResource(VerificationDocument document) {
        Path requestDirectory = uploadRoot.resolve(String.valueOf(document.getVerificationRequest().getId())).normalize();
        Path documentPath = requestDirectory.resolve(document.getStoredFileName()).normalize();

        if (!documentPath.startsWith(requestDirectory)) {
            throw new IllegalArgumentException("Invalid document path.");
        }

        try {
            Resource resource = new UrlResource(documentPath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("Document file was not found.");
            }
            return resource;
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException("Document file was not found.", exception);
        }
    }

    public VerificationRequest create(Long landId,
                                      String submittedByName,
                                      String notes,
                                      VerificationDocumentType documentType,
                                      MultipartFile[] documents) {
        Land land = landService.findById(landId)
                .orElseThrow(() -> new IllegalArgumentException("Land listing was not found."));

        if (requestRepository.existsByLandIdAndActiveTrueAndStatusIn(landId, ACTIVE_REVIEW_STATUSES)) {
            throw new IllegalArgumentException("This land already has an active verification request.");
        }

        VerificationRequest request = new VerificationRequest();
        request.setLand(land);
        request.setSubmittedByName(requiredText(submittedByName, "Submitted by is required."));
        request.setNotes(cleanOptional(notes));
        request.setStatus(VerificationStatus.PENDING);

        VerificationRequest saved = requestRepository.save(request);
        uploadDocuments(saved.getId(), documentType, documents);
        return saved;
    }

    public VerificationRequest startReview(Long id, User reviewer) {
        VerificationRequest request = getActiveRequest(id);

        if (request.getStatus() != VerificationStatus.PENDING
                && request.getStatus() != VerificationStatus.ADDITIONAL_INFO_REQUIRED) {
            throw new IllegalArgumentException("Only pending requests can be moved under review.");
        }

        request.setStatus(VerificationStatus.UNDER_REVIEW);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(null);
        return requestRepository.save(request);
    }

    public VerificationRequest approve(Long id, User reviewer) {
        VerificationRequest request = getActiveRequest(id);
        request.setStatus(VerificationStatus.APPROVED);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(reviewer);
        request.setRejectionReason(null);
        request.setAdditionalInfoRequest(null);
        VerificationRequest saved = requestRepository.save(request);
        landService.markAvailable(saved.getLand().getId());
        return saved;
    }

    public VerificationRequest reject(Long id, String rejectionReason, User reviewer) {
        VerificationRequest request = getActiveRequest(id);
        request.setStatus(VerificationStatus.REJECTED);
        request.setRejectionReason(requiredText(rejectionReason, "Rejection reason is required."));
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(reviewer);
        VerificationRequest saved = requestRepository.save(request);
        landService.keepNonPublic(saved.getLand().getId());
        return saved;
    }

    public VerificationRequest requestAdditionalInfo(Long id, String message, User reviewer) {
        VerificationRequest request = getActiveRequest(id);
        request.setStatus(VerificationStatus.ADDITIONAL_INFO_REQUIRED);
        request.setAdditionalInfoRequest(requiredText(message, "Additional information message is required."));
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(reviewer);
        VerificationRequest saved = requestRepository.save(request);
        landService.keepNonPublic(saved.getLand().getId());
        return saved;
    }

    public VerificationRequest cancel(Long id) {
        VerificationRequest request = getActiveRequest(id);

        if (request.getStatus() == VerificationStatus.APPROVED || request.getStatus() == VerificationStatus.REJECTED) {
            throw new IllegalArgumentException("Reviewed verification history cannot be cancelled.");
        }

        request.setStatus(VerificationStatus.CANCELLED);
        request.setActive(false);
        return requestRepository.save(request);
    }

    public void uploadDocuments(Long requestId,
                                VerificationDocumentType documentType,
                                MultipartFile[] files) {
        VerificationRequest request = getActiveRequest(requestId);

        if (files == null || files.length == 0) {
            return;
        }

        VerificationDocumentType safeType = documentType == null ? VerificationDocumentType.OTHER : documentType;
        Path requestDirectory = uploadRoot.resolve(String.valueOf(request.getId())).normalize();

        if (!requestDirectory.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Invalid upload path.");
        }

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            validateDocument(file);
            String extension = getExtension(file.getOriginalFilename());
            String storedFileName = UUID.randomUUID() + "." + extension;
            Path target = requestDirectory.resolve(storedFileName).normalize();

            if (!target.startsWith(requestDirectory)) {
                throw new IllegalArgumentException("Invalid document path.");
            }

            try {
                Files.createDirectories(requestDirectory);
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to save verification document.", exception);
            }

            VerificationDocument document = new VerificationDocument();
            document.setVerificationRequest(request);
            document.setDocumentType(safeType);
            document.setOriginalFileName(cleanFileName(file.getOriginalFilename()));
            document.setStoredFileName(storedFileName);
            document.setFileUrl("/verification/documents/pending");
            document.setMimeType(file.getContentType());
            document.setFileSize(file.getSize());

            VerificationDocument savedDocument = documentRepository.save(document);
            savedDocument.setFileUrl("/verification/documents/" + savedDocument.getId());
            documentRepository.save(savedDocument);
        }
    }

    public void removeDocument(Long requestId, Long documentId) {
        VerificationDocument document = documentRepository.findById(documentId)
                .filter(VerificationDocument::isActive)
                .filter(item -> item.getVerificationRequest().getId().equals(requestId))
                .orElseThrow(() -> new IllegalArgumentException("Document was not found."));

        document.setActive(false);
        documentRepository.save(document);
    }

    @Transactional(readOnly = true)
    public boolean hasApprovedVerification(Long landId) {
        return requestRepository.findTopByLandIdAndStatusAndActiveTrueOrderByReviewedAtDescIdDesc(
                landId,
                VerificationStatus.APPROVED
        ).isPresent();
    }

    @Transactional(readOnly = true)
    public Set<Long> findApprovedLandIds(Collection<Land> lands) {
        List<Long> landIds = lands.stream()
                .map(Land::getId)
                .toList();

        if (landIds.isEmpty()) {
            return Set.of();
        }

        return requestRepository.findByLandIdInAndStatusAndActiveTrue(landIds, VerificationStatus.APPROVED)
                .stream()
                .map(request -> request.getLand().getId())
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public long countByStatus(VerificationStatus status) {
        return requestRepository.countByStatusAndActiveTrue(status);
    }

    private VerificationRequest getActiveRequest(Long id) {
        return requestRepository.findById(id)
                .filter(VerificationRequest::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Verification request was not found."));
    }

    private void validateDocument(MultipartFile file) {
        if (file.getSize() > MAX_DOCUMENT_SIZE) {
            throw new IllegalArgumentException("Each document must be 10MB or smaller.");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Only PDF, JPG, JPEG, PNG, and WEBP documents are allowed.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Uploaded document type is not allowed.");
        }
    }

    private String getExtension(String filename) {
        String cleanName = cleanFileName(filename);
        if (!cleanName.contains(".")) {
            return "";
        }

        return cleanName.substring(cleanName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String cleanFileName(String filename) {
        String cleanName = StringUtils.cleanPath(filename == null ? "document" : filename);
        String onlyName = Paths.get(cleanName).getFileName().toString();
        return onlyName.replaceAll("[^A-Za-z0-9._ -]", "_");
    }

    private String requiredText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private String cleanOptional(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
