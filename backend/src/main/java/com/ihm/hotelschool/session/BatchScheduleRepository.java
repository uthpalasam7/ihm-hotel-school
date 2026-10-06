package com.ihm.hotelschool.session;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchScheduleRepository extends JpaRepository<BatchSchedule, Long> {
    @EntityGraph(attributePaths = {"defaultLecturer"})
    Page<BatchSchedule> findByBatchId(Long batchId, Pageable pageable);
    @EntityGraph(attributePaths = {"defaultLecturer"})
    List<BatchSchedule> findByBatchIdAndStatusOrderByDayOfWeekAscStartTimeAscIdAsc(Long batchId, ScheduleStatus status, Pageable limit);
    Optional<BatchSchedule> findByIdAndBatchId(Long id, Long batchId);
    boolean existsByBatchIdAndStatus(Long batchId, ScheduleStatus status);
}
