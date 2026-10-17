package com.agroconnect.dto;

import jakarta.validation.constraints.Size;

/** Optional reason sent when rejecting a request. */
public record DecisionRequest(@Size(max = 300) String reason) { }
