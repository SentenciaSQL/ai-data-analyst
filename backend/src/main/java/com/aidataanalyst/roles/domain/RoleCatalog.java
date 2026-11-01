package com.aidataanalyst.roles.domain;

import java.util.List;

public final class RoleCatalog {

    private RoleCatalog() {
    }

    public static final List<String> ALL = List.of(
            "tenant:read",
            "tenant:write",
            "users:read",
            "users:write",
            "roles:read",
            "audit:read",
            "connections:read",
            "connections:write",
            "catalog:read",
            "catalog:write",
            "conversations:read",
            "conversations:write",
            "queries:read",
            "dashboards:read",
            "dashboards:write",
            "billing:read",
            "billing:write");

    public static List<String> permissionsFor(RoleCode code) {
        return switch (code) {
            case OWNER -> ALL;
            case ADMIN -> ALL.stream().filter(permission -> !"billing:write".equals(permission)).toList();
            case ANALYST -> List.of(
                    "tenant:read",
                    "connections:read",
                    "catalog:read",
                    "conversations:read",
                    "conversations:write",
                    "queries:read",
                    "dashboards:read",
                    "dashboards:write");
            case VIEWER -> List.of(
                    "tenant:read",
                    "connections:read",
                    "catalog:read",
                    "conversations:read",
                    "queries:read",
                    "dashboards:read");
        };
    }

    public static String displayName(RoleCode code) {
        return switch (code) {
            case OWNER -> "Owner";
            case ADMIN -> "Admin";
            case ANALYST -> "Analyst";
            case VIEWER -> "Viewer";
        };
    }

    public static String description(RoleCode code) {
        return switch (code) {
            case OWNER -> "Full access to the workspace, including ownership and billing.";
            case ADMIN -> "Manages people and workspace settings. Cannot change the subscription.";
            case ANALYST -> "Asks questions and works with future dashboards. Cannot manage people.";
            case VIEWER -> "Read-only access to the analyst workspace.";
        };
    }
}
