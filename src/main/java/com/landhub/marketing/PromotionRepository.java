package com.landhub.marketing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    List<Promotion> findAllByOrderByCreatedAtDesc();

    List<Promotion> findByStatusOrderByCreatedAtDesc(PromotionStatus status);

    List<Promotion> findByActiveTrueAndStatusOrderByStartDateAsc(PromotionStatus status);

    long countByActiveTrueAndStatus(PromotionStatus status);
}
