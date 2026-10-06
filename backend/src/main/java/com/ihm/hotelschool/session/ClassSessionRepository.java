package com.ihm.hotelschool.session;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long>, JpaSpecificationExecutor<ClassSession> {
    @org.springframework.data.jpa.repository.Query("select s.batch.id from ClassSession s where s.id = :id")
    Optional<Long> findBatchIdBySessionId(Long id);

    boolean existsByBatchId(Long batchId);
    boolean existsByBatchIdAndSessionDateBeforeOrBatchIdAndSessionDateAfter(Long batchId, java.time.LocalDate from,
            Long sameBatchId, java.time.LocalDate to);

    @org.springframework.data.jpa.repository.Query("""
        select s from ClassSession s where s.batch.id = :batchId and
        ((s.sessionDate between :fromDate and :toDate) or (s.generationDate between :fromDate and :toDate))
        order by s.sessionDate, s.startTime, s.id
        """)
    @EntityGraph(attributePaths = {"lecturer", "sourceSchedule"})
    java.util.List<ClassSession> findGenerationContext(Long batchId, java.time.LocalDate fromDate,
            java.time.LocalDate toDate, Pageable limit);

    @Override
    @EntityGraph(attributePaths = {"batch", "batch.branch", "batch.course", "lecturer"})
    Page<ClassSession> findAll(Specification<ClassSession> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"batch", "batch.branch", "batch.course", "lecturer", "originalSession"})
    Optional<ClassSession> findWithBatchById(Long id);
}
