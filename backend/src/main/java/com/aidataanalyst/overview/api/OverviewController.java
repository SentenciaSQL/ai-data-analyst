package com.aidataanalyst.overview.api;

import com.aidataanalyst.overview.application.OverviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/overview")
public class OverviewController {

    private final OverviewService overview;

    public OverviewController(OverviewService overview) {
        this.overview = overview;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('tenant:read')")
    public OverviewResponse current() {
        return overview.current();
    }
}
