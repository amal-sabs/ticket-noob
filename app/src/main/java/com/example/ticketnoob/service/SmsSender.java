package com.example.ticketnoob.service;

import com.example.ticketnoob.repository.RepositoryCallback;

public interface SmsSender {
    void send(String to, String message, RepositoryCallback<Boolean> callback);
}