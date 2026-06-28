package com.ihm.hotelschool.user.dto;

import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(@Size(min = 8, max = 128) String temporaryPassword, String reason) {
}
