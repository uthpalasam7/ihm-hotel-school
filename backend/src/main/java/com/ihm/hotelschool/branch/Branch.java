package com.ihm.hotelschool.branch;

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
@Table(name = "branches")
public class Branch {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 30)
	private String code;

	@Column(nullable = false, length = 150)
	private String name;

	private String address;

	@Column(name = "contact_number", length = 30)
	private String contactNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BranchStatus status;

	@Column(name = "is_default", nullable = false)
	private boolean defaultBranch;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "created_by")
	private Long createdBy;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "updated_by")
	private Long updatedBy;

	@Version
	private Long version;

	protected Branch() {
	}

	public Branch(
			String code,
			String name,
			String address,
			String contactNumber,
			BranchStatus status,
			boolean defaultBranch,
			Instant now,
			Long actorUserId) {
		this.code = code;
		this.name = name;
		this.address = address;
		this.contactNumber = contactNumber;
		this.status = status;
		this.defaultBranch = defaultBranch;
		this.createdAt = now;
		this.createdBy = actorUserId;
		this.updatedAt = now;
		this.updatedBy = actorUserId;
		this.version = 0L;
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getAddress() {
		return address;
	}

	public String getContactNumber() {
		return contactNumber;
	}

	public BranchStatus getStatus() {
		return status;
	}

	public boolean isDefaultBranch() {
		return defaultBranch;
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

	public void updateDetails(String code, String name, String address, String contactNumber, BranchStatus status, Instant now, Long actorUserId) {
		this.code = code;
		this.name = name;
		this.address = address;
		this.contactNumber = contactNumber;
		this.status = status;
		this.updatedAt = now;
		this.updatedBy = actorUserId;
	}

	public void changeStatus(BranchStatus status, Instant now, Long actorUserId) {
		this.status = status;
		this.updatedAt = now;
		this.updatedBy = actorUserId;
	}
}
