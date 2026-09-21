package com.ihm.hotelschool.card;

import com.ihm.hotelschool.audit.AuditService;
import com.ihm.hotelschool.branch.Branch;
import com.ihm.hotelschool.branch.BranchRepository;
import com.ihm.hotelschool.common.security.CurrentActorService;
import com.ihm.hotelschool.common.security.CurrentActorService.CurrentActor;
import com.ihm.hotelschool.common.web.ConflictException;
import com.ihm.hotelschool.common.web.NotFoundException;
import com.ihm.hotelschool.common.web.PageResponse;
import com.ihm.hotelschool.enrollment.EnrollmentRepository;
import com.ihm.hotelschool.student.Student;
import com.ihm.hotelschool.student.StudentRepository;
import com.ihm.hotelschool.student.StudentStatus;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentCardService {
    private final StudentRepository students;
    private final EnrollmentRepository enrollments;
    private final StudentCardRepository cards;
    private final StudentCardEventRepository events;
    private final BranchRepository branches;
    private final CurrentActorService actors;
    private final AuditService audit;
    private final CardTokenCodec codec;
    private final StudentCardRenderer renderer;
    private final Clock clock;
    private final String contactLine;

    StudentCardService(StudentRepository students, EnrollmentRepository enrollments,
            StudentCardRepository cards, StudentCardEventRepository events, BranchRepository branches,
            CurrentActorService actors, AuditService audit, CardTokenCodec codec,
            StudentCardRenderer renderer, Clock clock,
            @org.springframework.beans.factory.annotation.Value("${app.card.contact-line}") String contactLine) {
        this.students=students; this.enrollments=enrollments; this.cards=cards; this.events=events;
        this.branches=branches; this.actors=actors; this.audit=audit; this.codec=codec;
        this.renderer=renderer; this.clock=clock; this.contactLine=contactLine;
    }

    @Transactional
    public Response issue(Long studentId, Optional<Long> activeBranchId, Authentication auth, HttpServletRequest request) {
        CurrentActor actor=admin(auth);
        Student student=students.findForCardById(studentId).orElseThrow(()->new NotFoundException("Student was not found"));
        Branch branch=authorizedBranch(studentId,activeBranchId,actor);
        if (student.getStatus()!=StudentStatus.ACTIVE) throw new IllegalArgumentException("Activate the student before issuing a card");
        StudentCard existing=cards.findByStudentId(studentId).orElse(null);
        if (existing!=null && "ACTIVE".equals(existing.getStatus())) return response(existing);
        Instant now=clock.instant();
        StudentCard card;
        String action;
        if (existing==null) { card=cards.saveAndFlush(new StudentCard(student,codec.issue(),now,actor.id())); action="ISSUED"; }
        else { card=existing; card.reissue(codec.issue(),now,actor.id()); cards.flush(); action="REPLACED"; }
        events.save(new StudentCardEvent(card,action,null,now,actor.id()));
        Response result=response(card);
        audit.record(actor.user(),branch,"STUDENT_CARD_"+action,"StudentCard",card.getId(),null,result,null,request);
        return result;
    }

    @Transactional
    public Response replace(Long studentId, String reason, Optional<Long> activeBranchId, Authentication auth, HttpServletRequest request) {
        String why=reason(reason);
        CurrentActor actor=admin(auth);
        Student student=students.findForCardById(studentId).orElseThrow(()->new NotFoundException("Student was not found"));
        Branch branch=authorizedBranch(studentId,activeBranchId,actor);
        if (student.getStatus()!=StudentStatus.ACTIVE) throw new IllegalArgumentException("Activate the student before replacing a card");
        StudentCard card=activeCard(studentId);
        Response before=response(card);
        Instant now=clock.instant(); card.replace(codec.issue(),now,actor.id()); cards.flush();
        events.save(new StudentCardEvent(card,"REPLACED",why,now,actor.id()));
        Response after=response(card);
        audit.record(actor.user(),branch,"STUDENT_CARD_REPLACED","StudentCard",card.getId(),before,after,why,request);
        return after;
    }

    @Transactional
    public Response revoke(Long studentId, String reason, Optional<Long> activeBranchId, Authentication auth, HttpServletRequest request) {
        String why=reason(reason);
        CurrentActor actor=admin(auth);
        students.findForCardById(studentId).orElseThrow(()->new NotFoundException("Student was not found"));
        Branch branch=authorizedBranch(studentId,activeBranchId,actor);
        StudentCard card=activeCard(studentId);
        Response before=response(card);
        Instant now=clock.instant(); card.revoke(now,actor.id()); cards.flush();
        events.save(new StudentCardEvent(card,"REVOKED",why,now,actor.id()));
        Response after=response(card);
        audit.record(actor.user(),branch,"STUDENT_CARD_REVOKED","StudentCard",card.getId(),before,after,why,request);
        return after;
    }

    @Transactional(readOnly=true)
    public Optional<Response> get(Long studentId, Optional<Long> activeBranchId, Authentication auth) {
        CurrentActor actor=admin(auth);
        students.findById(studentId).orElseThrow(()->new NotFoundException("Student was not found"));
        List<Long> ids=enrollments.findBranchIdsByStudentId(studentId);
        if (ids.isEmpty()) return Optional.empty();
        authorizedBranch(ids,activeBranchId,actor);
        return cards.findByStudentId(studentId).map(this::response);
    }

    @Transactional(readOnly=true)
    public PageResponse<StudentCardEvent.Response> history(Long studentId, Optional<Long> activeBranchId, Authentication auth, Pageable pageable) {
        StudentCard card=accessibleCard(studentId,activeBranchId,auth);
        return PageResponse.from(events.findByCardId(card.getId(),PageRequest.of(pageable.getPageNumber(),
                Math.min(pageable.getPageSize(),100),Sort.by("occurredAt").descending().and(Sort.by("id").descending()))),StudentCardEvent::response);
    }

    @Transactional(readOnly=true)
    public byte[] qr(Long studentId, Optional<Long> activeBranchId, Authentication auth) {
        StudentCard card=accessibleCard(studentId,activeBranchId,auth);
        if (!"ACTIVE".equals(card.getStatus())) throw new ConflictException("Card is revoked");
        return renderer.qrPng(codec.unseal(card));
    }

    @Transactional(readOnly=true)
    public byte[] pdf(Long studentId, Optional<Long> activeBranchId, Authentication auth) {
        StudentCard card=accessibleCard(studentId,activeBranchId,auth);
        if (!"ACTIVE".equals(card.getStatus())) throw new ConflictException("Card is revoked");
        return renderer.pdf(card,codec.unseal(card));
    }

    @Transactional(readOnly=true)
    public AuthorizedCard authorizeForDelivery(Long studentId, Optional<Long> activeBranchId, Authentication auth) {
        CurrentActor actor=admin(auth);
        students.findById(studentId).orElseThrow(()->new NotFoundException("Student was not found"));
        Branch branch=authorizedBranch(studentId,activeBranchId,actor);
        StudentCard card=activeCard(studentId);
        return new AuthorizedCard(card,branch);
    }
    public record AuthorizedCard(StudentCard card, Branch branch) {}

    private StudentCard accessibleCard(Long studentId, Optional<Long> activeBranchId, Authentication auth) {
        CurrentActor actor=admin(auth);
        students.findById(studentId).orElseThrow(()->new NotFoundException("Student was not found"));
        authorizedBranch(studentId,activeBranchId,actor);
        return cards.findByStudentId(studentId).orElseThrow(()->new NotFoundException("Student card was not found"));
    }
    private StudentCard activeCard(Long studentId) {
        StudentCard card=cards.findByStudentId(studentId).orElseThrow(()->new NotFoundException("Student card was not found"));
        if (!"ACTIVE".equals(card.getStatus())) throw new ConflictException("Card is already revoked");
        return card;
    }
    private Branch authorizedBranch(Long studentId, Optional<Long> activeBranchId, CurrentActor actor) {
        return authorizedBranch(enrollments.findBranchIdsByStudentId(studentId),activeBranchId,actor);
    }
    private Branch authorizedBranch(List<Long> branchIds, Optional<Long> activeBranchId, CurrentActor actor) {
        if (branchIds.isEmpty()) throw new IllegalArgumentException("Enroll the student before issuing a card");
        Long id;
        if (activeBranchId.isPresent()) {
            id=activeBranchId.get();
            if (!branchIds.contains(id)) throw new AccessDeniedException("Access denied");
        } else {
            id=branchIds.stream().filter(b->actor.superAdmin() || actor.branchIds().contains(b))
                    .findFirst().orElseThrow(()->new AccessDeniedException("Access denied"));
        }
        actor.requireBranchAccess(List.of(id));
        return branches.findById(id).orElseThrow(()->new NotFoundException("Branch was not found"));
    }
    private CurrentActor admin(Authentication auth) {
        CurrentActor actor=actors.actor(auth); actor.requireAnyRole("SUPER_ADMIN","ADMIN"); return actor;
    }
    private String reason(String reason) {
        if (reason==null || reason.isBlank()) throw new IllegalArgumentException("A reason is required");
        if (reason.length()>500) throw new IllegalArgumentException("Reason is too long");
        return reason.trim();
    }
    private Response response(StudentCard card) {
        return new Response(card.getId(),card.getStudent().getId(),"IHM-ST-"+
                String.format(java.util.Locale.ROOT,"%06d",card.getStudent().getId()),
                card.getStatus(),card.getIssuedAt(),card.getRevokedAt(),card.getVersion(),contactLine);
    }
    public record Response(Long id, Long studentId, String identifier, String status,
            Instant issuedAt, Instant revokedAt, Long version, String contactLine) {}
}
