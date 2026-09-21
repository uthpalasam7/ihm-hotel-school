package com.ihm.hotelschool.student;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {
	boolean existsByNormalizedNic(String normalizedNic);
	boolean existsByNormalizedNicAndIdNot(String normalizedNic, Long id);
	Optional<Student> findByNormalizedNic(String normalizedNic);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
    @org.springframework.data.jpa.repository.Query("select s from Student s where s.id = :id")
    Optional<Student> findForEnrollmentById(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Student s where s.id = :id")
    Optional<Student> findForCardById(@org.springframework.data.repository.query.Param("id") Long id);
}
