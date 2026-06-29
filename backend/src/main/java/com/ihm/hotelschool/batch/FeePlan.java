package com.ihm.hotelschool.batch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "fee_plans")
public class FeePlan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "batch_id", nullable = false)
	private CourseBatch batch;

	@Column(name = "currency_code", nullable = false, length = 3)
	private String currencyCode;

	@Column(name = "registration_fee", nullable = false, precision = 14, scale = 2)
	private BigDecimal registrationFee;

	@Column(name = "course_fee", nullable = false, precision = 14, scale = 2)
	private BigDecimal courseFee;

	@Column(name = "examination_fee", nullable = false, precision = 14, scale = 2)
	private BigDecimal examinationFee;

	@Column(name = "duration_months", nullable = false)
	private Integer durationMonths;

	@Column(name = "monthly_due_day", nullable = false)
	private Integer monthlyDueDay;

	@Column(name = "examination_due_date", nullable = false)
	private LocalDate examinationDueDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private FeePlanStatus status;

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

	protected FeePlan() {
	}

	public FeePlan(CourseBatch batch, String currencyCode, BigDecimal registrationFee, BigDecimal courseFee,
			BigDecimal examinationFee, Integer durationMonths, Integer monthlyDueDay, LocalDate examinationDueDate,
			FeePlanStatus status, Instant now, Long actorUserId) {
		this.batch = batch;
		this.currencyCode = currencyCode;
		this.registrationFee = registrationFee;
		this.courseFee = courseFee;
		this.examinationFee = examinationFee;
		this.durationMonths = durationMonths;
		this.monthlyDueDay = monthlyDueDay;
		this.examinationDueDate = examinationDueDate;
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

	public CourseBatch getBatch() {
		return batch;
	}

	public String getCurrencyCode() {
		return currencyCode;
	}

	public BigDecimal getRegistrationFee() {
		return registrationFee;
	}

	public BigDecimal getCourseFee() {
		return courseFee;
	}

	public BigDecimal getExaminationFee() {
		return examinationFee;
	}

	public Integer getDurationMonths() {
		return durationMonths;
	}

	public Integer getMonthlyDueDay() {
		return monthlyDueDay;
	}

	public LocalDate getExaminationDueDate() {
		return examinationDueDate;
	}

	public FeePlanStatus getStatus() {
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

	public void updateDetails(String currencyCode, BigDecimal registrationFee, BigDecimal courseFee,
			BigDecimal examinationFee, Integer durationMonths, Integer monthlyDueDay, LocalDate examinationDueDate,
			FeePlanStatus status, Instant now, Long actorUserId) {
		this.currencyCode = currencyCode;
		this.registrationFee = registrationFee;
		this.courseFee = courseFee;
		this.examinationFee = examinationFee;
		this.durationMonths = durationMonths;
		this.monthlyDueDay = monthlyDueDay;
		this.examinationDueDate = examinationDueDate;
		this.status = status;
		this.updatedAt = now;
		this.updatedBy = actorUserId;
	}
}
