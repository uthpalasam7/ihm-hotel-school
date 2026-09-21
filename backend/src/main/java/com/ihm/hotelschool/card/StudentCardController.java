package com.ihm.hotelschool.card;

import com.ihm.hotelschool.common.security.ActiveBranchContextService;
import com.ihm.hotelschool.common.web.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/students/{studentId}/card")
class StudentCardController {
    private final StudentCardService service;
    private final ActiveBranchContextService branches;
    StudentCardController(StudentCardService service,ActiveBranchContextService branches) { this.service=service; this.branches=branches; }

    @GetMapping
    ResponseEntity<StudentCardService.Response> get(@PathVariable Long studentId, Authentication auth, HttpServletRequest request) {
        Optional<StudentCardService.Response> card=service.get(studentId,branches.activeBranchId(request,auth),auth);
        return card.map(ResponseEntity::ok).orElseGet(()->ResponseEntity.noContent().build());
    }
    @PostMapping
    StudentCardService.Response issue(@PathVariable Long studentId, Authentication auth, HttpServletRequest request) {
        return service.issue(studentId,branches.activeBranchId(request,auth),auth,request);
    }
    @PostMapping("/replace")
    StudentCardService.Response replace(@PathVariable Long studentId,@Valid @RequestBody Reason body,
            Authentication auth,HttpServletRequest request) {
        return service.replace(studentId,body.reason(),branches.activeBranchId(request,auth),auth,request);
    }
    @PostMapping("/revoke")
    StudentCardService.Response revoke(@PathVariable Long studentId,@Valid @RequestBody Reason body,
            Authentication auth,HttpServletRequest request) {
        return service.revoke(studentId,body.reason(),branches.activeBranchId(request,auth),auth,request);
    }
    @GetMapping("/history")
    PageResponse<StudentCardEvent.Response> history(@PathVariable Long studentId,Pageable pageable,
            Authentication auth,HttpServletRequest request) {
        return service.history(studentId,branches.activeBranchId(request,auth),auth,pageable);
    }
    @GetMapping(value="/qr",produces=MediaType.IMAGE_PNG_VALUE)
    ResponseEntity<byte[]> qr(@PathVariable Long studentId,Authentication auth,HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG)
                .body(service.qr(studentId,branches.activeBranchId(request,auth),auth));
    }
    @GetMapping(value="/pdf",produces=MediaType.APPLICATION_PDF_VALUE)
    ResponseEntity<byte[]> pdf(@PathVariable Long studentId,Authentication auth,HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=ihm-student-card-"+studentId+".pdf")
                .body(service.pdf(studentId,branches.activeBranchId(request,auth),auth));
    }
    public record Reason(@NotBlank @Size(max=500) String reason) {}
}
