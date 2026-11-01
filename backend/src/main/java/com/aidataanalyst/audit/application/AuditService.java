package com.aidataanalyst.audit.application;

import com.aidataanalyst.audit.infrastructure.AuditEventEntity;
import com.aidataanalyst.audit.infrastructure.AuditEventRepository;
import com.aidataanalyst.shared.tenant.TenantContext;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditEventRepository events;

    public AuditService(AuditEventRepository events) {
        this.events = events;
    }

    @Transactional
    public void record(String action, String resourceType, String resourceId, Map<String, Object> metadata) {
        save(action, resourceType, resourceId, metadata);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIndependently(String action, String resourceType, String resourceId, Map<String, Object> metadata) {
        save(action, resourceType, resourceId, metadata);
    }

    private void save(String action, String resourceType, String resourceId, Map<String, Object> metadata) {
        AuditEventEntity event = new AuditEventEntity();
        event.setId(UUID.randomUUID());
        event.setTenantId(TenantContext.requireTenantId());
        event.setActorUserId(TenantContext.userId().orElse(null));
        event.setAction(action);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        event.setMetadata(metadata == null ? Map.of() : metadata);
        events.save(event);
    }
}
