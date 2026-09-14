package com.landhub.verification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerificationDocumentRepository extends JpaRepository<VerificationDocument, Long> {

    List<VerificationDocument> findByVerificationRequestIdAndActiveTrueOrderByUploadedAtDesc(Long verificationRequestId);
}
