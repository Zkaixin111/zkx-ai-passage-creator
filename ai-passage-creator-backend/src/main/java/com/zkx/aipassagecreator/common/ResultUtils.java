package com.zkx.aipassagecreator.common;


import com.zkx.aipassagecreator.exception.ErrorCode;

/**
 * 响应构造工具类，快速生成统一格式的 BaseResponse
 */
public class ResultUtils {

    /**
     * 成功响应
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok");
    }

    /**
     * 失败响应（使用预定义错误码）
     */
    public static BaseResponse<?> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败响应（自定义状态码和消息）
     */
    public static BaseResponse<?> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }

    /**
     * 失败响应（预定义错误码 + 自定义消息）
     */
    public static BaseResponse<?> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }
}