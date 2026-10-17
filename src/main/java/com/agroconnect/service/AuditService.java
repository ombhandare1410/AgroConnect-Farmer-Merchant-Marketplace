package com.agroconnect.service;

import com.agroconnect.entity.AuditLog;
import com.agroconnect.entity.User;
import com.agroconnect.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository logs;

    public AuditService(AuditLogRepository logs) { this.logs = logs; }

    public void log(User actor, String action, String targetType, Long targetId, String details) {
        AuditLog l = new AuditLog();
        l.setActor(actor);
        l.setAction(action);
        l.setTargetType(targetType);
        l.setTargetId(targetId);
        l.setDetails(details == null ? null : details.length() > 300 ? details.substring(0, 300) : details);
        logs.save(l);
    }
}
