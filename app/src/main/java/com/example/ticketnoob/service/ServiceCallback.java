package com.example.ticketnoob.service;

public interface ServiceCallback<T> {
    void onComplete(ServiceResult<T> result);
}