package com.ihm.hotelschool.session;

import com.ihm.hotelschool.common.security.ActiveBranchContextService;
import com.ihm.hotelschool.common.web.PageResponse;
import com.ihm.hotelschool.session.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/batches/{batchId}")
class ScheduleController {
    private final ScheduleService schedules;
    private final SessionGenerationService generation;
    private final ActiveBranchContextService branches;

    ScheduleController(ScheduleService schedules, SessionGenerationService generation, ActiveBranchContextService branches) {
        this.schedules = schedules; this.generation = generation; this.branches = branches;
    }

    @GetMapping("/schedules")
    PageResponse<ScheduleResponse> list(@PathVariable Long batchId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, Authentication auth, HttpServletRequest http) {
        branches.activeBranchId(http, auth);
        return schedules.list(batchId, page, size, auth);
    }

    @PostMapping("/schedules") @ResponseStatus(HttpStatus.CREATED)
    ScheduleResponse create(@PathVariable Long batchId, @Valid @RequestBody ScheduleRequest body,
            Authentication auth, HttpServletRequest http) {
        branches.activeBranchId(http, auth);
        return schedules.create(batchId, body, auth, http);
    }

    @PutMapping("/schedules/{id}")
    ScheduleResponse update(@PathVariable Long batchId, @PathVariable Long id, @Valid @RequestBody ScheduleRequest body,
            Authentication auth, HttpServletRequest http) {
        branches.activeBranchId(http, auth);
        return schedules.update(batchId, id, body, auth, http);
    }

    @DeleteMapping("/schedules/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@PathVariable Long batchId, @PathVariable Long id, @RequestParam Long version,
            Authentication auth, HttpServletRequest http) {
        branches.activeBranchId(http, auth);
        schedules.deactivate(batchId, id, version, auth, http);
    }

    @PostMapping("/sessions/preview")
    GenerationPreview preview(@PathVariable Long batchId, @Valid @RequestBody GenerationRequest body,
            Authentication auth, HttpServletRequest http) {
        branches.activeBranchId(http, auth);
        return generation.preview(batchId, body, auth);
    }

    @PostMapping("/sessions/generate")
    GenerationResult generate(@PathVariable Long batchId, @Valid @RequestBody GenerationRequest body,
            Authentication auth, HttpServletRequest http) {
        branches.activeBranchId(http, auth);
        return generation.generate(batchId, body, auth, http);
    }
}
