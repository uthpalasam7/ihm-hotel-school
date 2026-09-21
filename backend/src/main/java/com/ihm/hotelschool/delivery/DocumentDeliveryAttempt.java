package com.ihm.hotelschool.delivery;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="document_delivery_attempts")
public class DocumentDeliveryAttempt {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="delivery_id") private DocumentDelivery delivery;
    @Column(name="attempt_number",nullable=false) private int attemptNumber;
    @Column(nullable=false,length=16) private String outcome;
    @Column(name="occurred_at",nullable=false) private Instant occurredAt;
    @Column(length=200) private String detail;
    protected DocumentDeliveryAttempt() {}
    DocumentDeliveryAttempt(DocumentDelivery delivery,int number,String outcome,Instant when,String detail) {
        this.delivery=delivery; this.attemptNumber=number; this.outcome=outcome; this.occurredAt=when; this.detail=detail;
    }
}
