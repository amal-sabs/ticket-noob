package com.example.ticketnoob.service;

import com.example.ticketnoob.model.User;
import com.example.ticketnoob.repository.UserRepository;

public class LoginService {

    private final UserRepository userRepository;

    public LoginService(UserRepository repo) {
        this.userRepository = repo;
    }

    public ServiceResult<User> login(String emailOrPhone, String password) {

        if (emailOrPhone.isEmpty()) {
            return ServiceResult.error(
                    "Please provide email or phone number",
                    "emailOrPhone"
            );
        }

        if (password.isEmpty()) {
            return ServiceResult.error(
                    "Please provide a password",
                    "password"
            );
        }

        User user = userRepository.authenticate(emailOrPhone, password);

        if (user == null) {
            return ServiceResult.error(
                    "Invalid credentials",
                    "credentials"
            );
        }

        return ServiceResult.success(user);
    }
}