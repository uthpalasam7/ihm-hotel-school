package com.ihm.hotelschool.session;

import com.ihm.hotelschool.common.security.ActiveBranchContextService;
import com.ihm.hotelschool.common.web.PageResponse;
import com.ihm.hotelschool.session.dto.SessionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import com.ihm.hotelschool.session.dto.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sessions")
class SessionController {
    private final SessionService service;
    private final SessionWriteService writes;
    private final ActiveBranchContextService branches;

    SessionController(SessionService service, SessionWriteService writes, ActiveBranchContextService branches) {
        this.service = service;
        this.writes = writes;
        this.branches = branches;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SessionResponse create(@Valid @RequestBody SessionRequest body, Authentication auth, HttpServletRequest request) {
        branches.activeBranchId(request, auth);
        return writes.create(body, auth, request);
    }

    @PutMapping("/{id}")
    SessionResponse update(@PathVariable Long id, @Valid @RequestBody SessionRequest body,
            Authentication auth, HttpServletRequest request) {
        branches.activeBranchId(request, auth);
        return writes.update(id, body, auth, request);
    }

    @PostMapping("/{id}/cancel")
    SessionResponse cancel(@PathVariable Long id, @Valid @RequestBody SessionCancellationRequest body,
            Authentication auth, HttpServletRequest request) {
        branches.activeBranchId(request, auth);
        return writes.cancel(id, body, auth, request);
    }

    @PostMapping("/{id}/reschedule")
    @ResponseStatus(HttpStatus.CREATED)
    SessionRescheduleResponse reschedule(@PathVariable Long id, @Valid @RequestBody SessionRescheduleRequest body,
            Authentication auth, HttpServletRequest request) {
        branches.activeBranchId(request, auth);
        return writes.reschedule(id, body, auth, request);
    }

    @GetMapping
    PageResponse<SessionResponse> list(@RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long batchId, @RequestParam(required = false) Long lecturerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, Authentication auth, HttpServletRequest request) {
        Long activeBranch = branches.activeBranchId(request, auth).orElse(null);
        return service.list(branchId == null ? activeBranch : branchId, batchId, lecturerId,
                dateFrom, dateTo, status, page, size, auth);
    }

    @GetMapping("/{id}")
    SessionResponse get(@PathVariable Long id, Authentication auth, HttpServletRequest request) {
        branches.activeBranchId(request, auth);
        return service.get(id, auth);
    }
}
