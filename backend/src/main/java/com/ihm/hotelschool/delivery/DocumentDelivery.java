package com.ihm.hotelschool.delivery;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="document_deliveries")
public class DocumentDelivery {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="document_type",nullable=false,length=24) private String documentType;
    @Column(name="document_id",nullable=false) private Long documentId;
    @Column(name="document_version",nullable=false) private Long documentVersion;
    @Column(name="student_id",nullable=false) private Long studentId;
    @Column(name="branch_id",nullable=false) private Long branchId;
    @Column(name="recipient_email",nullable=false,length=200) private String recipientEmail;
    @Column(name="idempotency_key",nullable=false,length=36) private String idempotencyKey;
    @Column(nullable=false,length=16) private String status;
    @Column(nullable=false) private int attempts;
    @Column(name="next_attempt_at",nullable=false) private Instant nextAttemptAt;
    @Column(name="provider_accepted_at") private Instant providerAcceptedAt;
    @Column(name="last_error",length=200) private String lastError;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="created_by",nullable=false) private Long createdBy;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @Column(name="updated_by",nullable=false) private Long updatedBy;
    @Version private Long version;
    protected DocumentDelivery() {}
    DocumentDelivery(String type,Long documentId,Long documentVersion,Long studentId,Long branchId,
            String recipient,String key,Instant now,Long actor) {
        this.documentType=type; this.documentId=documentId; this.documentVersion=documentVersion;
        this.studentId=studentId; this.branchId=branchId; this.recipientEmail=recipient;
        this.idempotencyKey=key; this.status="QUEUED"; this.nextAttemptAt=now;
        this.createdAt=this.updatedAt=now; this.createdBy=this.updatedBy=actor; this.version=0L;
    }
    void begin(Instant now) { status="SENDING"; attempts++; updatedAt=now; }
    void accepted(Instant now) { status="ACCEPTED"; providerAcceptedAt=now; lastError=null; updatedAt=now; }
    void failed(Instant now,String detail,boolean retry) {
        status=retry && attempts<3 ? "QUEUED":"FAILED";
        nextAttemptAt=now.plusSeconds(300L*Math.max(attempts,1));
        lastError=detail; updatedAt=now;
    }
    public Response response() { return new Response(id,documentType,documentId,recipientEmail,status,attempts,
            nextAttemptAt,providerAcceptedAt,lastError,createdAt); }
    public Long getId() { return id; }
    public String getDocumentType() { return documentType; }
    public Long getDocumentId() { return documentId; }
    public Long getDocumentVersion() { return documentVersion; }
    public Long getStudentId() { return studentId; }
    public String getRecipientEmail() { return recipientEmail; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public int getAttempts() { return attempts; }
    public Long getCreatedBy() { return createdBy; }
    public Long getBranchId() { return branchId; }
    public record Response(Long id,String documentType,Long documentId,String recipientEmail,String status,
            int attempts,Instant nextAttemptAt,Instant providerAcceptedAt,String lastError,Instant createdAt) {}
}
