package com.xiancore.common.dto;

import lombok.*;

/**
 * API响应对象 - 统一的REST API响应格式
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ApiResponse<T> {

    /** 状态码 (0=成功, 非0=失败) */
    private Integer code;

    /** 状态消息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 时间戳 */
    private Long timestamp;

    /**
     * 创建成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(0)
                .message("Success")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
                .code(0)
                .message("Success")
                .data(null)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建失败响应
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建失败响应 (默认code=1)
     */
    public static <T> ApiResponse<T> error(String message) {
        return error(1, message);
    }
}
