package com.ihm.hotelschool.enrollment;

import com.ihm.hotelschool.common.security.ActiveBranchContextService;
import com.ihm.hotelschool.common.web.PageResponse;
import com.ihm.hotelschool.enrollment.dto.*;
import com.ihm.hotelschool.finance.StudentCharge;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
class EnrollmentController {
    private final EnrollmentService service;
    private final ActiveBranchContextService branches;
    EnrollmentController(EnrollmentService service, ActiveBranchContextService branches) {
        this.service = service; this.branches = branches;
    }

    @PostMapping("/enrollments") @ResponseStatus(HttpStatus.CREATED)
    EnrollmentResponse create(@Valid @RequestBody EnrollmentRequest body, Authentication auth, HttpServletRequest request) {
        branches.activeBranchId(request, auth);
        return service.create(body, auth, request);
    }

    @PostMapping("/enrollments/preview")
    EnrollmentPreview preview(@Valid @RequestBody EnrollmentRequest body, Authentication auth, HttpServletRequest request) {
        branches.activeBranchId(request, auth);
        return service.preview(body, auth);
    }

    @GetMapping("/enrollments")
    PageResponse<EnrollmentResponse> list(@RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long batchId, @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String status, @RequestParam(required = false) String search,
            Pageable pageable, Authentication auth, HttpServletRequest request) {
        var activeBranch = branches.activeBranchId(request, auth);
        return service.list(branchId == null ? activeBranch.orElse(null) : branchId,
                batchId, studentId, status, search, pageable, auth);
    }

    @PatchMapping("/enrollments/{id}/status")
    EnrollmentResponse changeStatus(@PathVariable Long id,@Valid @RequestBody EnrollmentStatusRequest body,
            Authentication auth,HttpServletRequest request) {
        branches.activeBranchId(request,auth);
        return service.changeStatus(id,body,auth,request);
    }

    @GetMapping("/enrollments/{id}")
    EnrollmentResponse get(@PathVariable Long id, Authentication auth, HttpServletRequest request) {
        branches.activeBranchId(request, auth);
        return service.get(id, auth);
    }

    @GetMapping("/enrollments/{id}/charges")
    PageResponse<StudentCharge.Response> charges(@PathVariable Long id, Pageable pageable,
            Authentication auth, HttpServletRequest request) {
        branches.activeBranchId(request, auth);
        return service.charges(id, pageable, auth);
    }

    @GetMapping("/students/{studentId}/enrollments")
    PageResponse<EnrollmentResponse> studentEnrollments(@PathVariable Long studentId, Pageable pageable,
            Authentication auth, HttpServletRequest request) {
        return service.list(branches.activeBranchId(request, auth).orElse(null), null, studentId, null, null, pageable, auth);
    }

    @GetMapping("/batches/{batchId}/enrollments")
    PageResponse<EnrollmentResponse> batchEnrollments(@PathVariable Long batchId, Pageable pageable,
            Authentication auth, HttpServletRequest request) {
        return service.list(branches.activeBranchId(request, auth).orElse(null), batchId, null, null, null, pageable, auth);
    }

    @GetMapping("/batches/{batchId}/students")
    PageResponse<BatchStudentResponse> batchStudents(@PathVariable Long batchId, Pageable pageable,
            Authentication auth, HttpServletRequest request) {
        branches.activeBranchId(request, auth);
        return service.roster(batchId, pageable, auth);
    }
}
