package com.ihm.hotelschool.card;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "student_card_events")
public class StudentCardEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "card_id") private StudentCard card;
    @Column(nullable = false, length = 16) private String action;
    @Column(length = 500) private String reason;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "actor_id", nullable = false) private Long actorId;
    protected StudentCardEvent() {}
    StudentCardEvent(StudentCard card, String action, String reason, Instant when, Long actorId) {
        this.card=card; this.action=action; this.reason=reason; this.occurredAt=when; this.actorId=actorId;
    }
    public Response response() { return new Response(id, action, reason, occurredAt, actorId); }
    public record Response(Long id, String action, String reason, Instant occurredAt, Long actorId) {}
}
