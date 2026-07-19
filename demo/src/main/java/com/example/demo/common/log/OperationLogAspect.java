package com.example.demo.common.log;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * 对 Service 实现类中的写操作进行统一日志记录。
 * 查询方法不进入此切面，避免列表、分页请求产生大量低价值日志。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogUtil operationLogUtil;

    @Around("within(com.example.demo.service.impl..*) && ("
            + "execution(public * create*(..)) || "
            + "execution(public * save*(..)) || "
            + "execution(public * update*(..)) || "
            + "execution(public * disable*(..)) || "
            + "execution(public * grant*(..)) || "
            + "execution(public * revoke*(..)) || "
            + "execution(public * assign*(..)) || "
            + "execution(public * transition*(..)) || "
            + "execution(public * addComment*(..))"
            + ")")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startNanos = System.nanoTime();
        String operation = operationName(joinPoint);
        try {
            Object result = joinPoint.proceed();
            operationLogUtil.success(operation, elapsedMs(startNanos));
            return result;
        } catch (Throwable throwable) {
            operationLogUtil.failure(operation, elapsedMs(startNanos), throwable);
            throw throwable;
        }
    }

    private String operationName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return joinPoint.getTarget().getClass().getSimpleName() + "." + signature.getName();
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
