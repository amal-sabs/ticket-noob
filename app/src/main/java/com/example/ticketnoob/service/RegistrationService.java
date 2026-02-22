package com.example.ticketnoob.service;

import com.example.ticketnoob.model.User;
import com.example.ticketnoob.repository.UserRepository;

import java.util.regex.Pattern;
public class RegistrationService {

    private final UserRepository userRepository;

    private static final Pattern EMAIL_REGEX =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final Pattern PHONE_REGEX =
            Pattern.compile("^\\d{10}$");
    public RegistrationService(UserRepository repo) {
        this.userRepository = repo;
    }

    public ServiceResult<User> register(String name, String email, String phone, String password) {

        if (email.isEmpty() && phone.isEmpty()) {
            return ServiceResult.error("Provide email OR phone", "email_phone");
        }

        if (!email.isEmpty() && !EMAIL_REGEX.matcher(email).matches()) {
            return ServiceResult.error("Invalid email format", "email");
        }

        if (!phone.isEmpty() && !PHONE_REGEX.matcher(phone).matches()) {
            return ServiceResult.error("Invalid phone format", "phone");
        }

        if (name.isEmpty()) {
            return ServiceResult.error("Name required", "name");
        }

        if (password.isEmpty()) {
            return ServiceResult.error("Password required", "password");
        }

        User user = new User(name, email, phone, password, "CUSTOMER");

        try {
            boolean success = userRepository.save(user);

            if (!success) {
                return ServiceResult.error("Registration failed", "repository");
            }
        } catch (Exception e){
            return ServiceResult.error("Registration failed", "repository");
        }

        return ServiceResult.success(user);
    }
}