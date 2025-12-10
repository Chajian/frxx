package com.xiancore.core.data;

/**
 * 数据访问异常
 * 统一封装数据层的异常
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(Throwable cause) {
        super(cause);
    }
}
