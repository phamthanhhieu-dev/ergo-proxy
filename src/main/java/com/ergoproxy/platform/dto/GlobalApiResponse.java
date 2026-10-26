package com.ergoproxy.platform.dto;

public class GlobalApiResponse<T> {

    private int status;
    private String code;
    private String message;
    private T data;

    public GlobalApiResponse(int status, String code, String message, T data) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> GlobalApiResponse<T> success(T data) {
        GlobalApiResponse<T> response = new GlobalApiResponse<>(0, "200", null, data);
        return response;
    }

    public static <T> GlobalApiResponse<T> error(String code, String message) {
        GlobalApiResponse<T> response = new GlobalApiResponse<>(1, code, message, null);
        return response;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
