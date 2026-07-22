package com.ihm.hotelschool.student;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {
	boolean existsByNormalizedNic(String normalizedNic);
	boolean existsByNormalizedNicAndIdNot(String normalizedNic, Long id);
	Optional<Student> findByNormalizedNic(String normalizedNic);
}
