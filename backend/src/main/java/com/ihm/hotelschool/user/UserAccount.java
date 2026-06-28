package com.ihm.hotelschool.user;

import com.ihm.hotelschool.branch.Branch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class UserAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String username;

	@Column(unique = true, length = 200)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "full_name", nullable = false, length = 200)
	private String fullName;

	@Column(name = "contact_number", length = 30)
	private String contactNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private UserStatus status;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

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

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "user_roles",
			joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new HashSet<>();

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "user_branches",
			joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "branch_id"))
	private Set<Branch> branches = new HashSet<>();

	protected UserAccount() {
	}

	public UserAccount(String username, String email, String passwordHash, String fullName, UserStatus status, Instant now) {
		this.username = username;
		this.email = email;
		this.passwordHash = passwordHash;
		this.fullName = fullName;
		this.status = status;
		this.createdAt = now;
		this.updatedAt = now;
		this.version = 0L;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getFullName() {
		return fullName;
	}

	public String getContactNumber() {
		return contactNumber;
	}

	public UserStatus getStatus() {
		return status;
	}

	public Instant getLastLoginAt() {
		return lastLoginAt;
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

	public Set<Role> getRoles() {
		return roles;
	}

	public Set<Branch> getBranches() {
		return branches;
	}

	public boolean isDisabled() {
		return status == UserStatus.DISABLED;
	}

	public boolean requiresPasswordChange() {
		return status == UserStatus.PASSWORD_CHANGE_REQUIRED;
	}

	public void markLogin(Instant now) {
		this.lastLoginAt = now;
		this.updatedAt = now;
	}

	public void changePassword(String passwordHash, Instant now) {
		this.passwordHash = passwordHash;
		this.status = UserStatus.ACTIVE;
		this.updatedAt = now;
	}

	public void updateProfile(String username, String email, String fullName, String contactNumber, Instant now, Long actorUserId) {
		this.username = username;
		this.email = email;
		this.fullName = fullName;
		this.contactNumber = contactNumber;
		this.updatedAt = now;
		this.updatedBy = actorUserId;
	}

	public void changeStatus(UserStatus status, Instant now, Long actorUserId) {
		this.status = status;
		this.updatedAt = now;
		this.updatedBy = actorUserId;
	}

	public void resetPassword(String passwordHash, Instant now, Long actorUserId) {
		this.passwordHash = passwordHash;
		this.status = UserStatus.PASSWORD_CHANGE_REQUIRED;
		this.updatedAt = now;
		this.updatedBy = actorUserId;
	}

	public void replaceRoles(Set<Role> roles, Instant now, Long actorUserId) {
		this.roles.clear();
		this.roles.addAll(roles);
		this.updatedAt = now;
		this.updatedBy = actorUserId;
	}

	public void replaceBranches(Set<Branch> branches, Instant now, Long actorUserId) {
		this.branches.clear();
		this.branches.addAll(branches);
		this.updatedAt = now;
		this.updatedBy = actorUserId;
	}

	public void addRole(Role role) {
		roles.add(role);
	}

	public void addBranch(Branch branch) {
		branches.add(branch);
	}

	public void setCreatedBy(Long actorUserId) {
		this.createdBy = actorUserId;
		this.updatedBy = actorUserId;
	}
}
