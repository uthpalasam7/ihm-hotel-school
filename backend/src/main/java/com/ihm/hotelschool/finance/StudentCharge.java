package com.ihm.hotelschool.finance;

import com.ihm.hotelschool.enrollment.Enrollment;
import com.ihm.hotelschool.enrollment.dto.EnrollmentPreview.Charge;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "student_charges")
public class StudentCharge {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "enrollment_id") private Enrollment enrollment;
    @Column(name = "charge_type", nullable = false, length = 30) private String chargeType;
    @Column(name = "installment_number", nullable = false) private Integer installmentNumber;
    @Column(nullable = false, length = 300) private String description;
    @Column(name = "due_date", nullable = false) private LocalDate dueDate;
    @Column(name = "original_amount", nullable = false, precision = 14, scale = 2) private BigDecimal originalAmount;
    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 2) private BigDecimal discountAmount;
    @Column(name = "waiver_amount", nullable = false, precision = 14, scale = 2) private BigDecimal waiverAmount;
    @Column(name = "final_payable_amount", nullable = false, precision = 14, scale = 2) private BigDecimal finalPayableAmount;
    @Column(name = "currency_code", nullable = false, length = 3) private String currencyCode;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "paid_at") private Instant paidAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "updated_by", nullable = false) private Long updatedBy;
    @Version private Long version;

    protected StudentCharge() {}

    public StudentCharge(Enrollment enrollment, Charge charge, Instant now, Long actorId) {
        this.enrollment = enrollment;
        this.chargeType = charge.type();
        this.installmentNumber = charge.installmentNumber() == null ? 0 : charge.installmentNumber();
        this.description = charge.description();
        this.dueDate = charge.dueDate();
        this.originalAmount = this.finalPayableAmount = charge.amount();
        this.discountAmount = this.waiverAmount = BigDecimal.ZERO;
        this.currencyCode = enrollment.getCurrencyCode();
        this.status = charge.amount().signum() == 0 ? "PAID" : "UPCOMING";
        this.paidAt = charge.amount().signum() == 0 ? now : null;
        this.createdAt = this.updatedAt = now;
        this.createdBy = this.updatedBy = actorId;
        this.version = 0L;
    }

    public Response toResponse(LocalDate today) {
        String effectiveStatus = switch (status) {
            case "PAID", "WAIVED", "CANCELLED" -> status;
            default -> dueDate.isBefore(today) ? "OVERDUE" : dueDate.equals(today) ? "DUE" : "UPCOMING";
        };
        return new Response(id, chargeType, installmentNumber == 0 ? null : installmentNumber,
                description, dueDate, originalAmount, discountAmount, waiverAmount, finalPayableAmount, currencyCode, effectiveStatus);
    }

    public record Response(Long id, String type, Integer installmentNumber, String description, LocalDate dueDate,
            BigDecimal originalAmount, BigDecimal discountAmount, BigDecimal waiverAmount,
            BigDecimal finalPayableAmount, String currencyCode, String status) {}
}
