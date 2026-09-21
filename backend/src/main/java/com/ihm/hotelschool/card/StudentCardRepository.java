package com.ihm.hotelschool.card;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentCardRepository extends JpaRepository<StudentCard, Long> {
    @EntityGraph(attributePaths = "student")
    Optional<StudentCard> findByStudentId(Long studentId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from StudentCard c where c.id = :id")
    Optional<StudentCard> findForDeliveryById(Long id);
    Optional<StudentCard> findByTokenHashAndStatus(String tokenHash, String status);
}
