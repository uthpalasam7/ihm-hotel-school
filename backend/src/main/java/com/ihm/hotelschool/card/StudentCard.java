package com.ihm.hotelschool.card;

import com.ihm.hotelschool.student.Student;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "student_cards")
public class StudentCard {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id") private Student student;
    @Column(name = "token_hash", nullable = false, length = 64) private String tokenHash;
    @Column(name = "token_ciphertext", nullable = false) private byte[] tokenCiphertext;
    @Column(name = "token_iv", nullable = false) private byte[] tokenIv;
    @Column(nullable = false, length = 12) private String status;
    @Column(name = "issued_at", nullable = false) private Instant issuedAt;
    @Column(name = "issued_by", nullable = false) private Long issuedBy;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "updated_by", nullable = false) private Long updatedBy;
    @Version private Long version;

    protected StudentCard() {}
    StudentCard(Student student, CardTokenCodec.SealedToken token, Instant now, Long actor) {
        this.student=student; this.status="ACTIVE"; setToken(token,now,actor);
    }
    void replace(CardTokenCodec.SealedToken token, Instant now, Long actor) {
        if (!"ACTIVE".equals(status)) throw new IllegalStateException("Only active cards can be replaced");
        setToken(token,now,actor);
    }
    void revoke(Instant now, Long actor) {
        if (!"ACTIVE".equals(status)) throw new IllegalStateException("Card is already revoked");
        status="REVOKED"; revokedAt=now; updatedAt=now; updatedBy=actor;
    }
    void reissue(CardTokenCodec.SealedToken token, Instant now, Long actor) {
        status="ACTIVE"; setToken(token,now,actor);
    }
    private void setToken(CardTokenCodec.SealedToken token, Instant now, Long actor) {
        tokenHash=token.hash(); tokenCiphertext=token.ciphertext(); tokenIv=token.iv();
        issuedAt=now; issuedBy=actor; revokedAt=null; updatedAt=now; updatedBy=actor;
    }
    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public String getTokenHash() { return tokenHash; }
    byte[] getTokenCiphertext() { return tokenCiphertext; }
    byte[] getTokenIv() { return tokenIv; }
    public String getStatus() { return status; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public Long getVersion() { return version; }
}
