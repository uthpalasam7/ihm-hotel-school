package com.ihm.hotelschool.batch;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CourseBatchRepository extends JpaRepository<CourseBatch, Long>, JpaSpecificationExecutor<CourseBatch> {
	@org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@org.springframework.data.jpa.repository.Query("select b from CourseBatch b where b.id = :id")
	Optional<CourseBatch> findForEnrollmentById(Long id);
	boolean existsByBatchNumber(String batchNumber);

	boolean existsByBatchNumberAndIdNot(String batchNumber, Long id);

	long countByCourseId(Long courseId);

	@EntityGraph(attributePaths = {"course", "branch"})
	Optional<CourseBatch> findWithCourseAndBranchById(Long id);
}
