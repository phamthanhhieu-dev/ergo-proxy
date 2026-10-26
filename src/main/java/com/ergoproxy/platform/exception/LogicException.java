package com.ergoproxy.platform.exception;

public class LogicException extends RuntimeException {

    private final String code;
    private final String messageKey;
    private final Object[] args;

    public LogicException(String code, String messageKey, Object... args) {
        super(messageKey);
        this.code = code;
        this.messageKey = messageKey;
        this.args = args;
    }

    public LogicException(String message, String code, String messageKey, Object[] args) {
        super(message);
        this.code = code;
        this.messageKey = messageKey;
        this.args = args;
    }

    public LogicException(String message, Throwable cause, String code, String messageKey, Object... args) {
        super(message, cause);
        this.code = code;
        this.messageKey = messageKey;
        this.args = args;
    }

    public LogicException(Throwable cause, String code, String messageKey, Object... args) {
        super(cause);
        this.code = code;
        this.messageKey = messageKey;
        this.args = args;
    }

    public LogicException(
        String message,
        Throwable cause,
        boolean enableSuppression,
        boolean writableStackTrace,
        String code,
        String messageKey,
        Object[] args
    ) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = code;
        this.messageKey = messageKey;
        this.args = args;
    }

    public String getCode() {
        return code;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }
}
