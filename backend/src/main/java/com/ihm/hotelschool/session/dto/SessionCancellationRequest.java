package com.ihm.hotelschool.session.dto;

import jakarta.validation.constraints.*;

public record SessionCancellationRequest(@NotBlank @Size(max = 2000) String reason,
        @NotNull @PositiveOrZero Long version) {}
