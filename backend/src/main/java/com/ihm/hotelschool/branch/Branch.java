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

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public BranchStatus getStatus() {
		return status;
	}

	public boolean isDefaultBranch() {
		return defaultBranch;
	}
}
