package com.ihm.hotelschool.user.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record UserRolesRequest(@NotEmpty Set<String> roleCodes, String reason) {
}
