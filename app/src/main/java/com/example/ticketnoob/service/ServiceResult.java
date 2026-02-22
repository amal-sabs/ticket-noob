package com.example.ticketnoob.service;

public class ServiceResult<T> {

    public final boolean success;
    public final String message;
    public final String field;
    public final T data;

    private ServiceResult(boolean success, String message, String field, T data) {
        this.success = success;
        this.message = message;
        this.field = field;
        this.data = data;
    }

    public static <T> ServiceResult<T> success(T data) {
        return new ServiceResult<>(true, null, null, data);
    }

    public static <T> ServiceResult<T> error(String message, String field) {
        return new ServiceResult<>(false, message, field, null);
    }
}