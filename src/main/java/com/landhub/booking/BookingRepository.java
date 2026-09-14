package com.landhub.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Booking> findByIdAndCustomerEmailIgnoreCase(Long id, String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select booking from Booking booking where booking.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    List<Booking> findAllByOrderByCreatedAtDesc();

    boolean existsByCustomerEmailIgnoreCaseAndLandIdAndStatusIn(String email, Long landId, Collection<BookingStatus> statuses);

    boolean existsByLandIdAndStatusIn(Long landId, Collection<BookingStatus> statuses);

    long countByStatus(BookingStatus status);
}
