package com.landhub.verification;

import com.landhub.auth.User;
import com.landhub.land.Land;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "verification_requests")
public class VerificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Land is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "land_id", nullable = false)
    private Land land;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private VerificationStatus status;

    @NotBlank(message = "Submitted by is required")
    @Size(max = 160, message = "Submitted by must be 160 characters or fewer")
    @Column(nullable = false, length = 160)
    private String submittedByName;

    @Size(max = 2000, message = "Notes must be 2000 characters or fewer")
    @Column(length = 2000)
    private String notes;

    @Size(max = 2000, message = "Rejection reason must be 2000 characters or fewer")
    @Column(length = 2000)
    private String rejectionReason;

    @Size(max = 2000, message = "Additional information request must be 2000 characters or fewer")
    @Column(length = 2000)
    private String additionalInfoRequest;

    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "verificationRequest", cascade = CascadeType.ALL)
    @OrderBy("uploadedAt DESC, id DESC")
    private List<VerificationDocument> documents = new ArrayList<>();

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (submittedAt == null) {
            submittedAt = now;
        }

        if (status == null) {
            status = VerificationStatus.PENDING;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isEditable() {
        return status == VerificationStatus.PENDING
                || status == VerificationStatus.UNDER_REVIEW
                || status == VerificationStatus.ADDITIONAL_INFO_REQUIRED;
    }

    public void addDocument(VerificationDocument document) {
        documents.add(document);
        document.setVerificationRequest(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Land getLand() {
        return land;
    }

    public void setLand(Land land) {
        this.land = land;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public void setStatus(VerificationStatus status) {
        this.status = status;
    }

    public String getSubmittedByName() {
        return submittedByName;
    }

    public void setSubmittedByName(String submittedByName) {
        this.submittedByName = submittedByName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getAdditionalInfoRequest() {
        return additionalInfoRequest;
    }

    public void setAdditionalInfoRequest(String additionalInfoRequest) {
        this.additionalInfoRequest = additionalInfoRequest;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<VerificationDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(List<VerificationDocument> documents) {
        this.documents = documents;
    }
}
