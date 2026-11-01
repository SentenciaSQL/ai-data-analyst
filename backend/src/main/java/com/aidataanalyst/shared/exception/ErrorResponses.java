package com.aidataanalyst.shared.exception;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;

public final class ErrorResponses {

    private ErrorResponses() {
    }

    public static void write(HttpServletResponse response, int status, String code, String message, String path)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"timestamp\":\"%s\",\"status\":%d,\"code\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}"
                .formatted(Instant.now(), status, escape(code), escape(message), escape(path)));
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", " ").replace("\n", " ");
    }
}
