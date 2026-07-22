package com.ihm.hotelschool.student.dto;

import com.ihm.hotelschool.student.StudentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StudentStatusRequest(@NotNull StudentStatus status, @Size(max = 1000) String reason) {
}
