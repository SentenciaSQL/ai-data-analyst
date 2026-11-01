package com.aidataanalyst.analyst.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalystMessageRequest(
        @NotBlank @Size(max = 4000) String message) {
}
