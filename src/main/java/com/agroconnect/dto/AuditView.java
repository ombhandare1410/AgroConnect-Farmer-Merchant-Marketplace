package com.agroconnect.dto;

import java.time.LocalDateTime;

public record AuditView(String actorName, String action, String targetType, Long targetId, String details,
                        LocalDateTime createdAt) { }
