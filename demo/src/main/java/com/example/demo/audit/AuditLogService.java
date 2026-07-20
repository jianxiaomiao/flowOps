package com.example.demo.audit;

import com.example.demo.entity.AuditLog;
import com.example.demo.mapper.AuditLogMapper;
import com.example.demo.security.CurrentUser;
import com.example.demo.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
/**
 * 持久化重要操作的审计记录。
 *
 * <p>使用独立事务，保证主业务随后回滚时，失败操作本身仍然可被追溯。</p>
 */
public class AuditLogService {
    private final AuditLogMapper auditLogMapper;
    private final CurrentUserProvider currentUserProvider;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    /**
     * 将一次已标记审计注解的业务操作写入 audit_log。
     *
     * @param metadata 注解声明的模块、操作和资源类型。
     * @param resourceId 成功时可提取出的业务资源 ID；失败时允许为空。
     * @param result 操作结果，例如 SUCCESS 或 FAILURE。
     * @param error 失败原因；成功时为 {@code null}。
     */
    public void record(AuditOperation metadata, Long resourceId, String result, Throwable error) {
        CurrentUser user = currentUserProvider.currentUserOrNull();
        if (user == null) {
            return;
        }
        AuditLog log = new AuditLog();
        log.setTenantId(user.tenantId());
        log.setOperatorId(user.userId());
        log.setModule(metadata.module());
        log.setOperation(metadata.operation());
        log.setResourceType(metadata.resourceType());
        log.setResourceId(resourceId);
        log.setTraceId(MDC.get("traceId"));
        log.setRequestIp(requestIp());
        log.setResult(result);
        log.setErrorMessage(error == null ? null : abbreviate(error.getMessage(), 500));
        log.setOccurredAt(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    /**
     * 从当前 Web 请求中提取远程 IP。
     *
     * @return 请求 IP；非 Web 线程中返回 {@code null}。
     */
    private String requestIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return abbreviate(request.getRemoteAddr(), 45);
        }
        return null;
    }

    /**
     * 截断字段值，避免数据库长度限制导致审计写入失败。
     *
     * @param value 原文本。
     * @param max 最大字符数。
     * @return 可安全保存的文本。
     */
    private String abbreviate(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max);
    }
}
