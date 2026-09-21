package com.ihm.hotelschool.delivery;

import com.ihm.hotelschool.audit.AuditService;
import com.ihm.hotelschool.branch.BranchRepository;
import com.ihm.hotelschool.card.CardTokenCodec;
import com.ihm.hotelschool.card.StudentCard;
import com.ihm.hotelschool.card.StudentCardRenderer;
import com.ihm.hotelschool.card.StudentCardRepository;
import com.ihm.hotelschool.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DocumentDeliveryDispatcher {
    private final DocumentDeliveryRepository deliveries;
    private final DocumentDeliveryAttemptRepository attempts;
    private final StudentCardRepository cards;
    private final StudentCardRenderer renderer;
    private final CardTokenCodec codec;
    private final ObjectProvider<JavaMailSender> senders;
    private final UserRepository users;
    private final BranchRepository branches;
    private final AuditService audit;
    private final Clock clock;
    private final String from;
    private final String host;
    private final TransactionTemplate transactions;
    DocumentDeliveryDispatcher(DocumentDeliveryRepository deliveries,DocumentDeliveryAttemptRepository attempts,
            StudentCardRepository cards,StudentCardRenderer renderer,CardTokenCodec codec,
            ObjectProvider<JavaMailSender> senders,UserRepository users,BranchRepository branches,AuditService audit,
            Clock clock,@Value("${app.delivery.from:}") String from,@Value("${spring.mail.host:}") String host,
            PlatformTransactionManager transactionManager) {
        this.deliveries=deliveries; this.attempts=attempts; this.cards=cards; this.renderer=renderer;
        this.codec=codec; this.senders=senders; this.users=users; this.branches=branches;
        this.audit=audit; this.clock=clock; this.from=from; this.host=host;
        this.transactions=new TransactionTemplate(transactionManager);
    }
    @Scheduled(fixedDelayString="${app.delivery.poll-ms:30000}")
    public void dispatchOne() {
        JavaMailSender sender=senders.getIfAvailable();
        if (sender==null || from.isBlank() || host.isBlank()) return;
        Long id=transactions.execute(status -> claim());
        if (id!=null) transactions.executeWithoutResult(status -> sendClaimed(id,sender));
    }
    private Long claim() {
        Instant now=clock.instant();
        for (DocumentDelivery stale : deliveries.findStaleSending(now.minusSeconds(600))) {
            stale.failed(now,"Delivery outcome uncertain; review before resending",false);
            audit(stale,"CARD_EMAIL_UNCERTAIN");
        }
        var ready=deliveries.findReady(now,PageRequest.of(0,1));
        if (ready.isEmpty()) return null;
        DocumentDelivery job=ready.get(0);
        job.begin(now); deliveries.flush();
        return job.getId();
    }
    private void sendClaimed(Long id,JavaMailSender sender) {
        DocumentDelivery job=deliveries.findLockedById(id).orElse(null);
        if (job==null || !"SENDING".equals(job.response().status())) return;
        StudentCard card=cards.findForDeliveryById(job.getDocumentId()).orElse(null);
        if (card==null || !"ACTIVE".equals(card.getStatus()) || !card.getVersion().equals(job.getDocumentVersion())) {
            fail(job,"Card changed or was revoked before delivery",false);
            return;
        }
        try {
            byte[] pdf=renderer.pdf(card,codec.unseal(card));
            var message=sender.createMimeMessage();
            var helper=new MimeMessageHelper(message,true,"UTF-8");
            helper.setFrom(from);
            helper.setTo(job.getRecipientEmail());
            helper.setSubject("IHM Hotel School student card");
            helper.setText("Please find your IHM Hotel School student card attached. Keep the QR code private and contact the school if the card is lost.");
            helper.addAttachment("ihm-student-card.pdf",new ByteArrayResource(pdf),"application/pdf");
            sender.send(message);
        } catch (MailException exception) {
            fail(job,"Mail service rejected or did not confirm delivery",true);
            return;
        } catch (Exception exception) {
            fail(job,"Card document could not be delivered",false);
            return;
        }
        Instant accepted=clock.instant(); job.accepted(accepted);
        attempts.save(new DocumentDeliveryAttempt(job,job.getAttempts(),"ACCEPTED",accepted,null));
        audit(job,"CARD_EMAIL_ACCEPTED");
    }
    private void fail(DocumentDelivery job,String detail,boolean retry) {
        Instant now=clock.instant(); job.failed(now,detail,retry);
        attempts.save(new DocumentDeliveryAttempt(job,job.getAttempts(),"FAILED",now,detail));
        audit(job,"CARD_EMAIL_FAILED");
    }
    private void audit(DocumentDelivery job,String action) {
        var actor=users.findById(job.getCreatedBy()).orElse(null);
        var branch=branches.findById(job.getBranchId()).orElse(null);
        if (actor!=null) audit.record(actor,branch,action,"DocumentDelivery",job.getId(),null,job.response(),null,null);
    }
}
