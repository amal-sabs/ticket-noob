package com.example.ticketnoob.service;

import com.example.ticketnoob.repository.RepositoryCallback;

public interface EmailSender {
    void send(String to, String subject, String body, RepositoryCallback<Boolean> callback);
}