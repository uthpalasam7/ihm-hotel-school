package com.ihm.hotelschool.student;

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
import java.time.LocalDate;

@Entity
@Table(name = "students")
public class Student {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "full_name", nullable = false, length = 250)
	private String fullName;

	@Column(nullable = false, length = 30)
	private String nic;

	@Column(name = "normalized_nic", nullable = false, unique = true, length = 30)
	private String normalizedNic;

	@Column(name = "contact_number", nullable = false, length = 30)
	private String contactNumber;

	@Column(name = "alternative_contact_number", length = 30)
	private String alternativeContactNumber;

	@Column(length = 200)
	private String email;

	@Column(nullable = false)
	private String address;

	@Column(name = "date_of_birth")
	private LocalDate dateOfBirth;

	@Column(length = 30)
	private String gender;

	@Column(name = "photo_storage_key", length = 500)
	private String photoStorageKey;

	private String remarks;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StudentStatus status;

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

	protected Student() {
	}

	Student(String fullName, String nic, String normalizedNic, String contactNumber,
			String alternativeContactNumber, String email, String address, LocalDate dateOfBirth,
			String gender, String remarks, Instant now, Long actorUserId) {
		this.fullName = fullName;
		this.nic = nic;
		this.normalizedNic = normalizedNic;
		this.contactNumber = contactNumber;
		this.alternativeContactNumber = alternativeContactNumber;
		this.email = email;
		this.address = address;
		this.dateOfBirth = dateOfBirth;
		this.gender = gender;
		this.remarks = remarks;
		this.status = StudentStatus.ACTIVE;
		this.createdAt = now;
		this.createdBy = actorUserId;
		this.updatedAt = now;
		this.updatedBy = actorUserId;
		this.version = 0L;
	}

	public Long getId() { return id; }
	public String getFullName() { return fullName; }
	public String getNic() { return nic; }
	public String getNormalizedNic() { return normalizedNic; }
	public String getContactNumber() { return contactNumber; }
	public String getAlternativeContactNumber() { return alternativeContactNumber; }
	public String getEmail() { return email; }
	public String getAddress() { return address; }
	public LocalDate getDateOfBirth() { return dateOfBirth; }
	public String getGender() { return gender; }
	public String getPhotoStorageKey() { return photoStorageKey; }
	public String getRemarks() { return remarks; }
	public StudentStatus getStatus() { return status; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public Long getVersion() { return version; }

	void updateDetails(String fullName, String nic, String normalizedNic, String contactNumber,
			String alternativeContactNumber, String email, String address, LocalDate dateOfBirth,
			String gender, String remarks, Instant now, Long actorUserId) {
		this.fullName = fullName;
		this.nic = nic;
		this.normalizedNic = normalizedNic;
		this.contactNumber = contactNumber;
		this.alternativeContactNumber = alternativeContactNumber;
		this.email = email;
		this.address = address;
		this.dateOfBirth = dateOfBirth;
		this.gender = gender;
		this.remarks = remarks;
		this.updatedAt = now;
		this.updatedBy = actorUserId;
	}

	void changeStatus(StudentStatus status, Instant now, Long actorUserId) {
		this.status = status;
		this.updatedAt = now;
		this.updatedBy = actorUserId;
	}

	void changePhoto(String storageKey, Instant now, Long actorUserId) {
		this.photoStorageKey = storageKey;
		this.updatedAt = now;
		this.updatedBy = actorUserId;
	}
}
