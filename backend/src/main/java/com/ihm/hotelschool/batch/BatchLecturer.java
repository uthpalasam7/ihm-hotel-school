package com.ihm.hotelschool.batch;

import com.ihm.hotelschool.user.UserAccount;
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
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "batch_lecturers")
public class BatchLecturer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "batch_id", nullable = false)
	private CourseBatch batch;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lecturer_user_id", nullable = false)
	private UserAccount lecturer;

	@Column(name = "assignment_start_date", nullable = false)
	private LocalDate assignmentStartDate;

	@Column(name = "assignment_end_date")
	private LocalDate assignmentEndDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BatchLecturerStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "created_by", nullable = false)
	private Long createdBy;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "updated_by", nullable = false)
	private Long updatedBy;

	protected BatchLecturer() {
	}

	public BatchLecturer(CourseBatch batch, UserAccount lecturer, LocalDate assignmentStartDate,
			LocalDate assignmentEndDate, BatchLecturerStatus status, Instant now, Long actorUserId) {
		this.batch = batch;
		this.lecturer = lecturer;
		this.assignmentStartDate = assignmentStartDate;
		this.assignmentEndDate = assignmentEndDate;
		this.status = status;
		this.createdAt = now;
		this.createdBy = actorUserId;
		this.updatedAt = now;
		this.updatedBy = actorUserId;
	}

	public Long getId() {
		return id;
	}

	public CourseBatch getBatch() {
		return batch;
	}

	public UserAccount getLecturer() {
		return lecturer;
	}

	public LocalDate getAssignmentStartDate() {
		return assignmentStartDate;
	}

	public LocalDate getAssignmentEndDate() {
		return assignmentEndDate;
	}

	public BatchLecturerStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void updateDetails(UserAccount lecturer, LocalDate assignmentStartDate, LocalDate assignmentEndDate,
			BatchLecturerStatus status, Instant now, Long actorUserId) {
		this.lecturer = lecturer;
		this.assignmentStartDate = assignmentStartDate;
		this.assignmentEndDate = assignmentEndDate;
		this.status = status;
		this.updatedAt = now;
		this.updatedBy = actorUserId;
	}

	public void changeStatus(BatchLecturerStatus status, Instant now, Long actorUserId) {
		this.status = status;
		this.updatedAt = now;
		this.updatedBy = actorUserId;
	}
}
