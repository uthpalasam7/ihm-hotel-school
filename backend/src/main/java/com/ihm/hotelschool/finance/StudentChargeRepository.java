package com.ihm.hotelschool.finance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentChargeRepository extends JpaRepository<StudentCharge, Long> {
    Page<StudentCharge> findByEnrollmentId(Long enrollmentId, Pageable pageable);
}
