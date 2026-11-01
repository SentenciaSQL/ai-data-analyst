package com.aidataanalyst.identity.application;

import jakarta.servlet.http.HttpServletRequest;

public record ClientRequestInfo(String ipAddress, String userAgent) {

    public static ClientRequestInfo from(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr()
                : forwarded.split(",")[0].trim();
        return new ClientRequestInfo(TextRules.clip(ip, 64), TextRules.clip(request.getHeader("User-Agent"), 500));
    }
}
