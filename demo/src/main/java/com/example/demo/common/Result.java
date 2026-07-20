package com.example.demo.common;

import java.io.Serial;
import java.io.Serializable;

/**
 * FlowOps 统一 API 响应体。
 *
 * @param <T> 响应中的业务数据类型
 */
public final class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final int SUCCESS_CODE = 0;
    public static final String SUCCESS_MSG = "success";


    private final int code;
    private final String msg;
    private final T data;

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /** @return 不携带业务数据的成功响应。 */
    public static Result<Void> success() {
        return new Result<>(SUCCESS_CODE, SUCCESS_MSG, null);
    }

    /** @param data 成功时返回的业务数据。@param <T> 数据类型。@return 携带数据的成功响应。 */
    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS_CODE, SUCCESS_MSG, data);
    }

    /** @param msg 自定义成功提示。@param data 成功时返回的业务数据。@param <T> 数据类型。@return 自定义提示的成功响应。 */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(SUCCESS_CODE, msg, data);
    }

    /** @param code 非零业务错误码。@param msg 可安全返回客户端的失败提示。@param <T> 数据类型。@return 不携带数据的失败响应。@throws IllegalArgumentException 当错误码为成功码 0 时抛出。 */
    public static <T> Result<T> failure(int code, String msg) {
        if (code == SUCCESS_CODE) {
            throw new IllegalArgumentException("失败响应的 code 不能为 0");
        }
        return new Result<>(code, msg, null);
    }

    /** @return 业务状态码；成功固定为 0。 */
    public int getCode() {
        return code;
    }

    /** @return 面向客户端的简短提示。 */
    public String getMsg() {
        return msg;
    }

    /** @return 成功时的业务数据；失败时通常为 {@code null}。 */
    public T getData() {
        return data;
    }
}
