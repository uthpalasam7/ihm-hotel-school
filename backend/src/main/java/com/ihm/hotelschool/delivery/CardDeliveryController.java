package com.ihm.hotelschool.delivery;

import com.ihm.hotelschool.common.security.ActiveBranchContextService;
import com.ihm.hotelschool.common.web.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
class CardDeliveryController {
    private final CardDeliveryService service;
    private final ActiveBranchContextService branches;
    CardDeliveryController(CardDeliveryService service,ActiveBranchContextService branches) {
        this.service=service; this.branches=branches;
    }
    @GetMapping("/document-deliveries/availability")
    CardDeliveryService.Availability availability(Authentication auth) { return service.availability(auth); }
    @PostMapping("/students/{studentId}/card/email")
    @ResponseStatus(HttpStatus.ACCEPTED)
    DocumentDelivery.Response queue(@PathVariable Long studentId,@Valid @RequestBody EmailRequest body,
            Authentication auth,HttpServletRequest request) {
        return service.queue(studentId,body.idempotencyKey(),branches.activeBranchId(request,auth),auth,request);
    }
    @GetMapping("/students/{studentId}/card/deliveries")
    PageResponse<DocumentDelivery.Response> history(@PathVariable Long studentId,Pageable pageable,
            Authentication auth,HttpServletRequest request) {
        return service.list(studentId,branches.activeBranchId(request,auth),auth,pageable);
    }
    record EmailRequest(@NotBlank String idempotencyKey) {}
}
