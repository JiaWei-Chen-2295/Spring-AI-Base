package com.example.aitemplate.api.error;

import java.time.Instant;

public record ApiError(String errorCode, String message, boolean retryable, Instant timestamp) {}
