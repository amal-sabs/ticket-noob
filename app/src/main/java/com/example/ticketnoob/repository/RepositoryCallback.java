package com.example.ticketnoob.repository;

public interface RepositoryCallback<T> {
    void onComplete(T result, String error);
}