package com.example.demo.common.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 统一写入业务操作日志。
 *
 * <p>这里只记录操作名、结果与耗时，绝不序列化方法参数，避免密码、工单正文等敏感信息落盘。</p>
 */
@Component
public class OperationLogUtil {

    private static final Logger OPERATION_LOGGER = LoggerFactory.getLogger("FLOWOPS_OPERATION");

    public void success(String operation, long elapsedMs) {
        OPERATION_LOGGER.info("operation={} result=SUCCESS elapsedMs={}", operation, elapsedMs);
    }

    public void failure(String operation, long elapsedMs, Throwable throwable) {
        OPERATION_LOGGER.warn(
                "operation={} result=FAILURE elapsedMs={} errorType={} errorMessage={}",
                operation,
                elapsedMs,
                throwable.getClass().getSimpleName(),
                safeMessage(throwable.getMessage())
        );
    }

    private String safeMessage(String message) {
        if (message == null) {
            return "";
        }
        return message.length() <= 200 ? message : message.substring(0, 200);
    }
}
