package com.ihm.hotelschool.session.dto;

import java.util.List;

public record GenerationResult(int createdCount, int skippedCount, List<Long> createdSessionIds) {}
