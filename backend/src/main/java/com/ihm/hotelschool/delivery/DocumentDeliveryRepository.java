package com.ihm.hotelschool.delivery;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface DocumentDeliveryRepository extends JpaRepository<DocumentDelivery,Long> {
    Optional<DocumentDelivery> findByIdempotencyKey(String key);
    @Query("select d from DocumentDelivery d where d.status='SENDING' and d.updatedAt < :before")
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    List<DocumentDelivery> findStaleSending(Instant before);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DocumentDelivery d where d.id = :id")
    Optional<DocumentDelivery> findLockedById(Long id);
    Page<DocumentDelivery> findByStudentIdAndDocumentTypeAndDocumentId(Long studentId,String type,Long documentId,Pageable pageable);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DocumentDelivery d where d.status='QUEUED' and d.nextAttemptAt <= :now order by d.id")
    List<DocumentDelivery> findReady(Instant now,Pageable pageable);
}
