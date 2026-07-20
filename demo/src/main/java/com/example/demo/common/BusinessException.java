package com.example.demo.common;

/** 可预期的业务失败，例如数据不存在、重复提交或状态冲突。 */
/**
 * 表示可预期的业务失败，例如参数不合法、资源不存在、权限不足或并发冲突。
 *
 * <p>全局异常处理器会将其中的业务码映射为正确的 HTTP 状态和统一响应。</p>
 */
public class BusinessException extends RuntimeException {

    private final int code;

    /**
     * 创建一个携带业务错误码的异常。
     *
     * @param code FlowOps 业务错误码。
     * @param message 可安全返回给客户端的错误提示。
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /** @return 本次业务失败对应的业务错误码。 */
    public int getCode() {
        return code;
    }
}
