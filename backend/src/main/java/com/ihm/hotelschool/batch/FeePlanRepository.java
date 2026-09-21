package com.ihm.hotelschool.batch;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeePlanRepository extends JpaRepository<FeePlan, Long> {
	@org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
	@org.springframework.data.jpa.repository.Query("select f from FeePlan f where f.batch.id = :batchId")
	Optional<FeePlan> findForEnrollmentByBatchId(Long batchId);
	@EntityGraph(attributePaths = {"batch", "batch.course", "batch.branch"})
	Optional<FeePlan> findByBatchId(Long batchId);
}
