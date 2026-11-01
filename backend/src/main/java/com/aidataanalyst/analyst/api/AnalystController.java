package com.aidataanalyst.analyst.api;

import com.aidataanalyst.analyst.application.AnalystStreamService;
import com.aidataanalyst.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1/analyst")
public class AnalystController {

    private final AnalystStreamService analyst;

    public AnalystController(AnalystStreamService analyst) {
        this.analyst = analyst;
    }

    @PostMapping(value = "/messages:stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('conversations:write')")
    public ResponseEntity<StreamingResponseBody> stream(@Valid @RequestBody AnalystMessageRequest request) {
        StreamingResponseBody body = analyst.stream(
                TenantContext.requireTenantId(),
                TenantContext.requireUserId(),
                request.message().trim());
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(body);
    }
}
