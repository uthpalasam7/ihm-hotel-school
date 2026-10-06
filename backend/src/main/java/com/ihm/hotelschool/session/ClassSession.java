package com.ihm.hotelschool.session;

import com.ihm.hotelschool.batch.CourseBatch;
import com.ihm.hotelschool.user.UserAccount;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "class_sessions")
public class ClassSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "batch_id")
    private CourseBatch batch;
    @Column(nullable = false) private LocalDate sessionDate;
    // Class times are local wall-clock values; do not apply the UTC timestamp setting.
    @JdbcTypeCode(SqlTypes.LOCAL_TIME)
    @Column(nullable = false) private LocalTime startTime;
    @JdbcTypeCode(SqlTypes.LOCAL_TIME)
    @Column(nullable = false) private LocalTime endTime;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "lecturer_user_id")
    private UserAccount lecturer;
    @Column(length = 300) private String topic;
    @Column(length = 150) private String classroom;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private SessionStatus status;
    @Column(length = 2000) private String cancellationReason;
    @Column(length = 2000) private String reschedulingReason;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "original_session_id")
    private ClassSession originalSession;
    @Column(length = 2000) private String remarks;
    private Instant attendanceSubmittedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "source_schedule_id")
    private BatchSchedule sourceSchedule;
    private LocalDate generationDate;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Long createdBy;
    @Column(nullable = false) private Instant updatedAt;
    @Column(nullable = false) private Long updatedBy;
    @Version private Long version;

    protected ClassSession() {}

    public ClassSession(CourseBatch batch, LocalDate date, LocalTime start, LocalTime end,
            UserAccount lecturer, String topic, String classroom, String remarks, Instant now, Long actorId) {
        SessionValidation.date(batch, date);
        SessionValidation.times(start, end);
        this.batch = batch;
        this.sessionDate = date;
        this.startTime = start;
        this.endTime = end;
        this.lecturer = lecturer;
        this.topic = SessionValidation.text(topic, 300, "Topic");
        this.classroom = SessionValidation.text(classroom, 150, "Classroom");
        this.remarks = SessionValidation.text(remarks, 2000, "Remarks");
        this.status = SessionStatus.SCHEDULED;
        this.createdAt = this.updatedAt = now;
        this.createdBy = this.updatedBy = actorId;
        this.version = 0L;
    }

    public static ClassSession generated(BatchSchedule schedule, LocalDate date, Instant now, Long actorId) {
        ClassSession session = new ClassSession(schedule.getBatch(), date, schedule.getStartTime(), schedule.getEndTime(),
                schedule.getDefaultLecturer(), null, schedule.getClassroom(), null, now, actorId);
        session.sourceSchedule = schedule;
        session.generationDate = date;
        return session;
    }

    public BatchSchedule getSourceSchedule() { return sourceSchedule; }
    public LocalDate getGenerationDate() { return generationDate; }

    public void update(LocalDate date, LocalTime start, LocalTime end, UserAccount lecturer,
            String topic, String classroom, String remarks, Instant now, Long actorId) {
        if (status != SessionStatus.SCHEDULED || attendanceSubmittedAt != null) {
            throw new com.ihm.hotelschool.common.web.ConflictException(
                    "Only scheduled sessions without submitted attendance can be edited");
        }
        SessionValidation.date(batch, date);
        SessionValidation.times(start, end);
        this.sessionDate = date;
        this.startTime = start;
        this.endTime = end;
        this.lecturer = lecturer;
        this.topic = SessionValidation.text(topic, 300, "Topic");
        this.classroom = SessionValidation.text(classroom, 150, "Classroom");
        this.remarks = SessionValidation.text(remarks, 2000, "Remarks");
        this.updatedAt = now;
        this.updatedBy = actorId;
        // Source schedule/date and creation audit fields deliberately retain their original identity.
    }

    public void requireLifecycleChange() {
        if (status != SessionStatus.SCHEDULED || attendanceSubmittedAt != null) {
            throw new com.ihm.hotelschool.common.web.ConflictException(
                    "Only scheduled sessions without submitted attendance can be cancelled or rescheduled");
        }
    }

    public void cancel(String reason, Instant now, Long actorId) {
        requireLifecycleChange();
        this.cancellationReason = requiredReason(reason);
        this.status = SessionStatus.CANCELLED;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public ClassSession reschedule(LocalDate date, LocalTime start, LocalTime end, UserAccount lecturer,
            String reason, Instant now, Long actorId) {
        requireLifecycleChange();
        String validatedReason = requiredReason(reason);
        if (sessionDate.equals(date) && startTime.equals(start) && endTime.equals(end)) {
            throw new IllegalArgumentException("Rescheduling must change the session date or time");
        }
        var replacement = new ClassSession(batch, date, start, end, lecturer, topic, classroom, remarks, now, actorId);
        replacement.originalSession = this;
        // The original keeps its generation slot; the replacement traces back through originalSession.
        this.reschedulingReason = validatedReason;
        this.status = SessionStatus.RESCHEDULED;
        this.updatedAt = now;
        this.updatedBy = actorId;
        return replacement;
    }

    public void requireAttendanceEligible() {
        if (status != SessionStatus.SCHEDULED && status != SessionStatus.COMPLETED) {
            throw new com.ihm.hotelschool.common.web.ConflictException(
                    "Attendance cannot be recorded for cancelled or rescheduled sessions");
        }
    }

    private static String requiredReason(String value) {
        String reason = SessionValidation.text(value, 2000, "Reason");
        if (reason == null) throw new IllegalArgumentException("Reason is required");
        return reason;
    }

    public String getReschedulingReason() { return reschedulingReason; }
    public Long getId() { return id; }
    public CourseBatch getBatch() { return batch; }
    public LocalDate getSessionDate() { return sessionDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public UserAccount getLecturer() { return lecturer; }
    public String getTopic() { return topic; }
    public String getClassroom() { return classroom; }
    public SessionStatus getStatus() { return status; }
    public String getCancellationReason() { return cancellationReason; }
    public ClassSession getOriginalSession() { return originalSession; }
    public String getRemarks() { return remarks; }
    public Instant getAttendanceSubmittedAt() { return attendanceSubmittedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
