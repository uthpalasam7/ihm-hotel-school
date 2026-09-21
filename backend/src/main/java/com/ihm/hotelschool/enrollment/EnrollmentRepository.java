package com.ihm.hotelschool.enrollment;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long>, JpaSpecificationExecutor<Enrollment> {
    boolean existsByStudentIdAndBatchId(Long studentId, Long batchId);
    boolean existsByBatchId(Long batchId);
    long countByBatchId(Long batchId);

    @org.springframework.data.jpa.repository.Query("select distinct e.batch.branch.id from Enrollment e where e.student.id = :studentId")
    java.util.List<Long> findBranchIdsByStudentId(Long studentId);

    @Override
    @EntityGraph(attributePaths = {"student", "batch", "batch.course", "batch.branch"})
    Page<Enrollment> findAll(Specification<Enrollment> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "batch", "batch.course", "batch.branch"})
    Optional<Enrollment> findWithStudentAndBatchById(Long id);
}
