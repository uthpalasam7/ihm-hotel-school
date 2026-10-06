package com.ihm.hotelschool.session.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public record GenerationRequest(@NotNull LocalDate fromDate, @NotNull LocalDate toDate,
        @Size(max = 366) List<@NotNull LocalDate> excludeDates, @Size(max = 100) String previewToken) {
    public GenerationRequest {
        excludeDates = excludeDates == null ? List.of() : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(excludeDates));
    }
}
