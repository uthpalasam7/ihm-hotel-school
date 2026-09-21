package com.ihm.hotelschool.card;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentCardEventRepository extends JpaRepository<StudentCardEvent, Long> {
    Page<StudentCardEvent> findByCardId(Long cardId, Pageable pageable);
}
