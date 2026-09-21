package com.ihm.hotelschool.delivery;

import com.ihm.hotelschool.audit.AuditService;
import com.ihm.hotelschool.card.StudentCard;
import com.ihm.hotelschool.card.StudentCardRepository;
import com.ihm.hotelschool.card.StudentCardService;
import com.ihm.hotelschool.common.security.CurrentActorService;
import com.ihm.hotelschool.common.web.ConflictException;
import com.ihm.hotelschool.common.web.PageResponse;
import com.ihm.hotelschool.common.web.NotFoundException;
import com.ihm.hotelschool.common.web.ServiceUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardDeliveryService {
    private final DocumentDeliveryRepository deliveries;
    private final StudentCardService cards;
    private final StudentCardRepository cardRepository;
    private final CurrentActorService actors;
    private final AuditService audit;
    private final Clock clock;
    private final ObjectProvider<JavaMailSender> sender;
    private final String host;
    private final String from;
    CardDeliveryService(DocumentDeliveryRepository deliveries,StudentCardService cards,StudentCardRepository cardRepository,CurrentActorService actors,
            AuditService audit,Clock clock,ObjectProvider<JavaMailSender> sender,
            @Value("${spring.mail.host:}") String host,@Value("${app.delivery.from:}") String from) {
        this.deliveries=deliveries; this.cards=cards; this.cardRepository=cardRepository; this.actors=actors; this.audit=audit;
        this.clock=clock; this.sender=sender; this.host=host; this.from=from;
    }
    public boolean available() { return !host.isBlank() && !from.isBlank() && sender.getIfAvailable()!=null; }
    public Availability availability(Authentication auth) {
        actors.actor(auth).requireAnyRole("SUPER_ADMIN","ADMIN");
        return new Availability(available());
    }
    @Transactional
    public DocumentDelivery.Response queue(Long studentId,String idempotencyKey,Optional<Long> activeBranch,
            Authentication auth,HttpServletRequest request) {
        var actor=actors.actor(auth); actor.requireAnyRole("SUPER_ADMIN","ADMIN");
        UUID key;
        try { key=UUID.fromString(idempotencyKey); }
        catch (RuntimeException exception) { throw new IllegalArgumentException("A valid delivery request ID is required"); }
        var authorized=cards.authorizeForDelivery(studentId,activeBranch,auth);
        StudentCard card=cardRepository.findForDeliveryById(authorized.card().getId())
                .orElseThrow(()->new ConflictException("Card is no longer available"));
        if (!"ACTIVE".equals(card.getStatus())) throw new ConflictException("Card is revoked");
        String email=card.getStudent().getEmail();
        if (email==null || email.isBlank()) throw new IllegalArgumentException("Save a student email address before sending the card");
        Optional<DocumentDelivery> existing=deliveries.findByIdempotencyKey(key.toString());
        if (existing.isPresent()) {
            DocumentDelivery job=existing.get();
            if (!job.getStudentId().equals(studentId) || !job.getDocumentId().equals(card.getId()))
                throw new ConflictException("Delivery request ID was used for another document");
            return job.response();
        }
        if (!available()) throw new ServiceUnavailableException("Card email is not configured");
        var job=deliveries.saveAndFlush(new DocumentDelivery("STUDENT_CARD",card.getId(),card.getVersion(),
                studentId,authorized.branch().getId(),email,key.toString(),clock.instant(),actor.id()));
        audit.record(actor.user(),authorized.branch(),"CARD_EMAIL_QUEUED","DocumentDelivery",job.getId(),
                null,job.response(),null,request);
        return job.response();
    }
    @Transactional(readOnly=true)
    public PageResponse<DocumentDelivery.Response> list(Long studentId,Optional<Long> activeBranch,Authentication auth,Pageable pageable) {
        var card=cards.get(studentId,activeBranch,auth).orElseThrow(()->new NotFoundException("Student card was not found"));
        return PageResponse.from(deliveries.findByStudentIdAndDocumentTypeAndDocumentId(studentId,
                "STUDENT_CARD",card.id(),PageRequest.of(pageable.getPageNumber(),
                Math.min(pageable.getPageSize(),100),Sort.by("createdAt").descending())),DocumentDelivery::response);
    }
    public record Availability(boolean available) {}
}
