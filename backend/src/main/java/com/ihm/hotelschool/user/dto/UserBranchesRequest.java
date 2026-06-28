package com.ihm.hotelschool.user.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record UserBranchesRequest(@NotEmpty Set<Long> branchIds, String reason) {
}
