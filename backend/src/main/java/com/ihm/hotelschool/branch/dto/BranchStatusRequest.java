package com.ihm.hotelschool.branch.dto;

import com.ihm.hotelschool.branch.BranchStatus;
import jakarta.validation.constraints.NotNull;

public record BranchStatusRequest(@NotNull BranchStatus status, String reason) {
}
