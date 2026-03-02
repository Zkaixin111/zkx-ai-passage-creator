package com.zkx.aipassagecreator.exception;

import lombok.Getter;

/**
 * 自定义业务异常
 * <p>
 * 携带业务错误码 code，由 GlobalExceptionHandler 统一捕获并返回给前端。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码 */
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
