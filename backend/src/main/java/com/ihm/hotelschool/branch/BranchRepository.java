package com.ihm.hotelschool.branch;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BranchRepository extends JpaRepository<Branch, Long>, JpaSpecificationExecutor<Branch> {
	Optional<Branch> findByCode(String code);

	boolean existsByCode(String code);

	boolean existsByCodeAndIdNot(String code, Long id);

	boolean existsByIdAndStatus(Long id, BranchStatus status);

	long countByStatus(BranchStatus status);
}
