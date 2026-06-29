package com.ihm.hotelschool.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "courses")
public class Course {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(name = "short_code", nullable = false, unique = true, length = 20)
	private String shortCode;

	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CourseStatus status;

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

	protected Course() {
	}

	public Course(String name, String shortCode, String description, CourseStatus status, Instant now, Long actorUserId) {
		this.name = name;
		this.shortCode = shortCode;
		this.description = description;
		this.status = status;
		this.createdAt = now;
		this.createdBy = actorUserId;
		this.updatedAt = now;
		this.updatedBy = actorUserId;
		this.version = 0L;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getShortCode() {
		return shortCode;
	}

	public String getDescription() {
		return description;
	}

	public CourseStatus getStatus() {
		return status;
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

	public void updateDetails(String name, String shortCode, String description, CourseStatus status, Instant now, Long actorUserId) {
		this.name = name;
		this.shortCode = shortCode;
		this.description = description;
		this.status = status;
		this.updatedAt = now;
		this.updatedBy = actorUserId;
	}

	public void changeStatus(CourseStatus status, Instant now, Long actorUserId) {
		this.status = status;
		this.updatedAt = now;
		this.updatedBy = actorUserId;
	}
}
