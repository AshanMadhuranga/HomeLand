package com.landhub.land;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface LandRepository extends JpaRepository<Land, Long> {

    List<Land> findByStatusNotOrderByCreatedAtDesc(LandStatus status);

    List<Land> findByStatusInOrderByCreatedAtDesc(Collection<LandStatus> statuses);

    List<Land> findAllByOrderByCreatedAtDesc();
}
