package com.ihm.hotelschool.session;

import com.ihm.hotelschool.batch.CourseBatch;
import com.ihm.hotelschool.user.UserAccount;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "batch_schedules")
public class BatchSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "batch_id")
    private CourseBatch batch;
    @Column(nullable = false) private Integer dayOfWeek;
    // Class times are local wall-clock values; do not apply the UTC timestamp setting.
    @JdbcTypeCode(SqlTypes.LOCAL_TIME)
    @Column(nullable = false) private LocalTime startTime;
    @JdbcTypeCode(SqlTypes.LOCAL_TIME)
    @Column(nullable = false) private LocalTime endTime;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "default_lecturer_user_id")
    private UserAccount defaultLecturer;
    @Column(length = 150) private String classroom;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ScheduleStatus status;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Long createdBy;
    @Column(nullable = false) private Instant updatedAt;
    @Column(nullable = false) private Long updatedBy;
    @Version private Long version;

    protected BatchSchedule() {}

    public BatchSchedule(CourseBatch batch, int dayOfWeek, LocalTime start, LocalTime end,
            UserAccount lecturer, String classroom, Instant now, Long actorId) {
        if (batch == null) throw new IllegalArgumentException("Batch is required");
        if (dayOfWeek < 1 || dayOfWeek > 7) throw new IllegalArgumentException("Weekday must be between 1 and 7");
        SessionValidation.times(start, end);
        this.batch = batch;
        this.dayOfWeek = dayOfWeek;
        this.startTime = start;
        this.endTime = end;
        this.defaultLecturer = lecturer;
        this.classroom = SessionValidation.text(classroom, 150, "Classroom");
        this.status = ScheduleStatus.ACTIVE;
        this.createdAt = this.updatedAt = now;
        this.createdBy = this.updatedBy = actorId;
        this.version = 0L;
    }

    public void update(int day, LocalTime start, LocalTime end, UserAccount lecturer,
            String classroom, ScheduleStatus status, Instant now, Long actorId) {
        if (day < 1 || day > 7) throw new IllegalArgumentException("Weekday must be between 1 and 7");
        SessionValidation.times(start, end);
        this.dayOfWeek = day;
        this.startTime = start;
        this.endTime = end;
        this.defaultLecturer = lecturer;
        this.classroom = SessionValidation.text(classroom, 150, "Classroom");
        this.status = status;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void deactivate(Instant now, Long actorId) {
        this.status = ScheduleStatus.INACTIVE;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public Long getId() { return id; }
    public CourseBatch getBatch() { return batch; }
    public Integer getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public UserAccount getDefaultLecturer() { return defaultLecturer; }
    public String getClassroom() { return classroom; }
    public ScheduleStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
