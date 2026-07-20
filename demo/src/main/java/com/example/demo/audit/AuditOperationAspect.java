package com.example.demo.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
/**
 * 拦截 {@link AuditOperation} 标记的方法，并在成功或失败后记录审计日志。
 */
public class AuditOperationAspect {
    private final AuditLogService auditLogService;

    @Around("@annotation(metadata)")
    /**
     * 环绕执行目标业务方法，并保留原方法的返回值或异常。
     *
     * @param joinPoint 被拦截的方法调用上下文。
     * @param metadata 方法上的审计元数据。
     * @return 原业务方法的返回值。
     * @throws Throwable 原业务方法抛出的任何异常会在审计后继续向上抛出。
     */
    public Object audit(ProceedingJoinPoint joinPoint, AuditOperation metadata) throws Throwable {
        try {
            Object result = joinPoint.proceed();
            safeRecord(metadata, extractId(result), "SUCCESS", null);
            return result;
        } catch (Throwable throwable) {
            safeRecord(metadata, null, "FAILURE", throwable);
            throw throwable;
        }
    }

    /**
     * 尽力记录审计日志，避免审计基础设施故障掩盖原业务结果。
     *
     * @param metadata 审计元数据。
     * @param resourceId 资源 ID。
     * @param result 成功或失败结果。
     * @param error 原业务异常。
     */
    private void safeRecord(AuditOperation metadata, Long resourceId, String result, Throwable error) {
        try {
            auditLogService.record(metadata, resourceId, result, error);
        } catch (Exception auditError) {
            log.error("写入审计日志失败 module={} operation={}", metadata.module(), metadata.operation(), auditError);
        }
    }

    /**
     * 通过约定的 {@code getId()} 方法从返回实体中提取资源 ID。
     *
     * @param result 业务方法的返回值。
     * @return 数值型 ID；无法提取时返回 {@code null}。
     */
    private Long extractId(Object result) {
        if (result == null) return null;
        try {
            Method method = result.getClass().getMethod("getId");
            Object id = method.invoke(result);
            return id instanceof Number number ? number.longValue() : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
