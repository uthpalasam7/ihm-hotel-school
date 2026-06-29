package com.ihm.hotelschool.batch;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeePlanRepository extends JpaRepository<FeePlan, Long> {
	@EntityGraph(attributePaths = {"batch", "batch.course", "batch.branch"})
	Optional<FeePlan> findByBatchId(Long batchId);
}
