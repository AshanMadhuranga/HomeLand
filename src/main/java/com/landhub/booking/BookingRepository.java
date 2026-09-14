package com.landhub.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    Optional<Booking> findByIdAndCustomerEmailIgnoreCase(Long id, String email);

    List<Booking> findAllByOrderByCreatedAtDesc();

    boolean existsByCustomerEmailIgnoreCaseAndLandIdAndStatusIn(String email, Long landId, Collection<BookingStatus> statuses);

    boolean existsByLandIdAndStatusIn(Long landId, Collection<BookingStatus> statuses);

    long countByStatus(BookingStatus status);
}
