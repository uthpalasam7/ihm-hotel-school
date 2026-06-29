package com.ihm.hotelschool.batch;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchLecturerRepository extends JpaRepository<BatchLecturer, Long> {
	@EntityGraph(attributePaths = {"lecturer", "batch", "batch.branch", "batch.course"})
	List<BatchLecturer> findByBatchIdOrderByAssignmentStartDateAsc(Long batchId);

	@EntityGraph(attributePaths = {"lecturer", "batch", "batch.branch", "batch.course"})
	Optional<BatchLecturer> findByIdAndBatchId(Long id, Long batchId);

	@EntityGraph(attributePaths = {"lecturer", "batch", "batch.branch", "batch.course"})
	List<BatchLecturer> findByBatchIdAndStatusOrderByAssignmentStartDateAsc(Long batchId, BatchLecturerStatus status);

	@EntityGraph(attributePaths = {"lecturer", "batch", "batch.branch", "batch.course"})
	Optional<BatchLecturer> findByBatchIdAndLecturerIdAndStatus(Long batchId, Long lecturerId, BatchLecturerStatus status);

	long countByBatchIdAndStatus(Long batchId, BatchLecturerStatus status);
}
