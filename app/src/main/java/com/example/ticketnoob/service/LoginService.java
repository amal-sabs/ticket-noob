package com.example.ticketnoob.service;

import com.example.ticketnoob.model.User;
import com.example.ticketnoob.repository.UserRepository;

public class LoginService {

    private final UserRepository userRepository;

    public LoginService(UserRepository repo) {
        this.userRepository = repo;
    }

    public void login(String emailOrPhone,
                      String password,
                      ServiceCallback<User> callback) {

        if (emailOrPhone == null || emailOrPhone.trim().isEmpty()) {
            callback.onComplete(ServiceResult.error("Provide email or phone", "emailOrPhone"));
            return;
        }

        if (password == null || password.isEmpty()) {
            callback.onComplete(ServiceResult.error("Provide password", "password"));
            return;
        }

        userRepository.authenticate(emailOrPhone.trim(), password, (user, error) -> {
            if (user == null) {
                callback.onComplete(ServiceResult.error(
                        error != null ? error : "Invalid credentials",
                        "credentials"
                ));
                return;
            }

            callback.onComplete(ServiceResult.success(user));
        });
    }
}