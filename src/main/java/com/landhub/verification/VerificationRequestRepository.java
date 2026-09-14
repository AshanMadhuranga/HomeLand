package com.landhub.verification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, Long> {

    List<VerificationRequest> findAllByActiveTrueOrderBySubmittedAtDesc();

    List<VerificationRequest> findByStatusAndActiveTrueOrderBySubmittedAtDesc(VerificationStatus status);

    boolean existsByLandIdAndActiveTrueAndStatusIn(Long landId, Collection<VerificationStatus> statuses);

    long countByStatusAndActiveTrue(VerificationStatus status);

    Optional<VerificationRequest> findTopByLandIdAndStatusAndActiveTrueOrderByReviewedAtDescIdDesc(Long landId,
                                                                                                    VerificationStatus status);

    List<VerificationRequest> findByLandIdInAndStatusAndActiveTrue(Collection<Long> landIds, VerificationStatus status);
}
