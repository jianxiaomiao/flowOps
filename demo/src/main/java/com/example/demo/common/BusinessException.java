package com.example.demo.common;

/** 可预期的业务失败，例如数据不存在、重复提交或状态冲突。 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
