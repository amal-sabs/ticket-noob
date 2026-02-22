package com.example.ticketnoob.service;

import android.util.Patterns;

import com.example.ticketnoob.model.User;
import com.example.ticketnoob.repository.UserRepository;

import java.util.UUID;

public class RegistrationService {

    private final UserRepository userRepository;

    public RegistrationService(UserRepository repo) {
        this.userRepository = repo;
    }

    public void register(String name,
                         String email,
                         String phone,
                         String password,
                         ServiceCallback<User> callback) {

        name = safeTrim(name);
        email = safeTrim(email);
        phone = safeTrim(phone);

        if (email.isEmpty() && phone.isEmpty()) {
            callback.onComplete(ServiceResult.error("Provide email OR phone", "email_phone"));
            return;
        }

        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            callback.onComplete(ServiceResult.error("Invalid email format", "email"));
            return;
        }

        if (!phone.isEmpty() && !Patterns.PHONE.matcher(phone).matches()) {
            callback.onComplete(ServiceResult.error("Invalid phone format", "phone"));
            return;
        }

        if (name.isEmpty()) {
            callback.onComplete(ServiceResult.error("Name required", "name"));
            return;
        }

        if (password == null || password.isEmpty()) {
            callback.onComplete(ServiceResult.error("Password required", "password"));
            return;
        }

        User user = new User(name, email, phone, password, "CUSTOMER"); // Hard code customer for now

        userRepository.save(user, (success, error) -> {
            if (success == null || !success) {
                callback.onComplete(ServiceResult.error(
                        error != null ? error : "Registration failed",
                        "repository"
                ));
                return;
            }

            callback.onComplete(ServiceResult.success(user));
        });
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}