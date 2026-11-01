package com.aidataanalyst.shared.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldViolation> fields) {

    public record FieldViolation(String field, String message) {
    }
}
