package com.ihm.hotelschool.enrollment;

import com.ihm.hotelschool.batch.CourseBatch;
import com.ihm.hotelschool.batch.FeePlan;
import com.ihm.hotelschool.student.Student;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "enrollments")
public class Enrollment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id")
    private Student student;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "batch_id")
    private CourseBatch batch;
    @Column(name = "registration_number", nullable = false, length = 100)
    private String registrationNumber;
    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;
    @Column(name = "enrollment_date", nullable = false)
    private LocalDate enrollmentDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private EnrollmentStatus status;
    private String remarks;
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
    @Column(name = "batch_start_date", nullable = false)
    private LocalDate batchStartDate;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;
    @Version private Long version;

    protected Enrollment() {}

    public Enrollment(Student student, CourseBatch batch, int sequence, LocalDate enrollmentDate,
            String remarks, FeePlan fees, Instant now, Long actorId) {
        this.student = student;
        this.batch = batch;
        this.sequenceNumber = sequence;
        this.registrationNumber = batch.getBatchNumber() + "/" + String.format(java.util.Locale.ROOT, "%04d", sequence);
        this.enrollmentDate = enrollmentDate;
        this.status = EnrollmentStatus.ACTIVE;
        this.remarks = remarks;
        this.createdAt = this.updatedAt = now;
        this.createdBy = this.updatedBy = actorId;
        this.version = 0L;
        this.currencyCode = fees.getCurrencyCode();
        this.registrationFee = fees.getRegistrationFee();
        this.courseFee = fees.getCourseFee();
        this.examinationFee = fees.getExaminationFee();
        this.durationMonths = fees.getDurationMonths();
        this.monthlyDueDay = fees.getMonthlyDueDay();
        this.examinationDueDate = fees.getExaminationDueDate();
        this.batchStartDate = batch.getStartDate();
    }

    void changeStatus(EnrollmentStatus next, Instant now, Long actor) {
        if (status==next) return;
        boolean allowed = switch (status) {
            case ACTIVE -> next==EnrollmentStatus.SUSPENDED || next==EnrollmentStatus.WITHDRAWN
                    || next==EnrollmentStatus.COMPLETED || next==EnrollmentStatus.CANCELLED;
            case SUSPENDED -> next==EnrollmentStatus.ACTIVE || next==EnrollmentStatus.WITHDRAWN
                    || next==EnrollmentStatus.CANCELLED;
            case COMPLETED, WITHDRAWN, CANCELLED -> false;
        };
        if (!allowed) throw new IllegalArgumentException("This enrollment status change is not allowed");
        status=next; updatedAt=now; updatedBy=actor;
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public CourseBatch getBatch() { return batch; }
    public String getRegistrationNumber() { return registrationNumber; }
    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public EnrollmentStatus getStatus() { return status; }
    public String getRemarks() { return remarks; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }
    public String getCurrencyCode() { return currencyCode; }
    public BigDecimal getRegistrationFee() { return registrationFee; }
    public BigDecimal getCourseFee() { return courseFee; }
    public BigDecimal getExaminationFee() { return examinationFee; }
    public Integer getDurationMonths() { return durationMonths; }
    public Integer getMonthlyDueDay() { return monthlyDueDay; }
    public LocalDate getExaminationDueDate() { return examinationDueDate; }
    public LocalDate getBatchStartDate() { return batchStartDate; }
}
