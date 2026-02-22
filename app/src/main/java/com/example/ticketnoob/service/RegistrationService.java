package com.example.ticketnoob.service;

import android.util.Patterns;

import com.example.ticketnoob.model.User;
import com.example.ticketnoob.repository.UserRepository;

public class RegistrationService {

    private final UserRepository userRepository;

    public RegistrationService(UserRepository repo) {
        this.userRepository = repo;
    }

    public ServiceResult<User> register(String name, String email, String phone, String password) {

        if (email.isEmpty() && phone.isEmpty()) {
            return ServiceResult.error("Provide email OR phone", "email_phone");
        }

        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return ServiceResult.error("Invalid email format", "email");
        }

        if (!phone.isEmpty() && !Patterns.PHONE.matcher(phone).matches()) {
            return ServiceResult.error("Invalid phone format", "phone");
        }

        if (name.isEmpty()) {
            return ServiceResult.error("Name required", "name");
        }

        if (password.isEmpty()) {
            return ServiceResult.error("Password required", "password");
        }

        User user = new User(name, email, phone, password, "CUSTOMER");

        boolean success = userRepository.save(user);

        if (!success) {
            return ServiceResult.error("Registration failed", "repository");
        }

        return ServiceResult.success(user);
    }
}