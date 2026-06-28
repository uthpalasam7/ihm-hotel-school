package com.ihm.hotelschool.user.dto;

import com.ihm.hotelschool.user.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(@NotNull UserStatus status, String reason) {
}
