package com.aidataanalyst.audit.api;

import com.aidataanalyst.audit.infrastructure.AuditEventEntity;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        UUID actorUserId,
        String action,
        String resourceType,
        String resourceId,
        Map<String, Object> metadata,
        Instant createdAt) {

    public static AuditEventResponse from(AuditEventEntity event) {
        return new AuditEventResponse(
                event.getId(),
                event.getActorUserId(),
                event.getAction(),
                event.getResourceType(),
                event.getResourceId(),
                event.getMetadata(),
                event.getCreatedAt());
    }
}
