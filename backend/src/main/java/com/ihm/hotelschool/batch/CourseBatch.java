package com.ihm.hotelschool.batch;

import com.ihm.hotelschool.branch.Branch;
import com.ihm.hotelschool.course.Course;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "course_batches")
public class CourseBatch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "course_id", nullable = false)
	private Course course;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "branch_id", nullable = false)
	private Branch branch;

	@Column(name = "batch_number", nullable = false, unique = true, length = 60)
	private String batchNumber;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "duration_months", nullable = false)
	private Integer durationMonths;

	@Enumerated(EnumType.STRING)
	@Column(name = "schedule_mode", nullable = false, length = 20)
	private ScheduleMode scheduleMode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BatchStatus status;

	private String remarks;

	@Column(name = "registration_sequence", nullable = false)
	private Integer registrationSequence;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "created_by", nullable = false)
	private Long createdBy;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "updated_by", nullable = false)
	private Long updatedBy;

	@Version
	private Long version;

	@OneToMany(mappedBy = "batch")
	private Set<BatchLecturer> lecturerAssignments = new HashSet<>();

	protected CourseBatch() {
	}

	public CourseBatch(
			Course course,
			Branch branch,
			String batchNumber,
			LocalDate startDate,
			LocalDate endDate,
			Integer durationMonths,
			ScheduleMode scheduleMode,
			BatchStatus status,
			String remarks,
			Instant now,
			Long actorUserId) {
		this.course = course;
		this.branch = branch;
		this.batchNumber = batchNumber;
		this.startDate = startDate;
		this.endDate = endDate;
		this.durationMonths = durationMonths;
		this.scheduleMode = scheduleMode;
		this.status = status;
		this.remarks = remarks;
		this.registrationSequence = 0;
		this.createdAt = now;
		this.createdBy = actorUserId;
		this.updatedAt = now;
		this.updatedBy = actorUserId;
		this.version = 0L;
	}

	public Long getId() {
		return id;
	}

	public Course getCourse() {
		return course;
	}

	public Branch getBranch() {
		return branch;
	}

	public String getBatchNumber() {
		return batchNumber;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public Integer getDurationMonths() {
		return durationMonths;
	}

	public ScheduleMode getScheduleMode() {
		return scheduleMode;
	}

	public BatchStatus getStatus() {
		return status;
	}

	public String getRemarks() {
		return remarks;
	}

	public Integer getRegistrationSequence() {
		return registrationSequence;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Long getVersion() {
		return version;
	}

	public void updateDetails(
			Course course,
			Branch branch,
			String batchNumber,
			LocalDate startDate,
			LocalDate endDate,
			Integer durationMonths,
			ScheduleMode scheduleMode,
			BatchStatus status,
			String remarks,
			Instant now,
			Long actorUserId) {
		this.course = course;
		this.branch = branch;
		this.batchNumber = batchNumber;
		this.startDate = startDate;
		this.endDate = endDate;
		this.durationMonths = durationMonths;
		this.scheduleMode = scheduleMode;
		this.status = status;
		this.remarks = remarks;
		this.updatedAt = now;
		this.updatedBy = actorUserId;
	}

	public void changeStatus(BatchStatus status, Instant now, Long actorUserId) {
		this.status = status;
		this.updatedAt = now;
		this.updatedBy = actorUserId;
	}
}
