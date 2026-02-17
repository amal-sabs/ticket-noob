package com.example.ticketnoob.service;

import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.ticketnoob.repository.UserRepository;
import com.example.ticketnoob.model.User;


public class RegistrationService {

    private final EditText etName, etEmail, etPhone, etPassword;
    private final Button btnRegister;
    private final UserRepository userRepository;
    private Context context;

    public RegistrationService(Context context, UserRepository repo, EditText name, EditText email, EditText phone, EditText pass, Button reg) {
        this.etName = name;
        this.etEmail = email;
        this.etPhone = phone;
        this.etPassword = pass;
        this.btnRegister = reg;
        this.userRepository = repo;
        this.context = context.getApplicationContext();
    }

    public void init() {
        btnRegister.setOnClickListener(v -> handleRegistration());
    }

    private void handleRegistration() {

        User newUser = validateAndBuildUser();

        if (newUser != null) {

            boolean success = userRepository.save(newUser);

            if (success) {
                Toast.makeText(context, "Success!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Registration failed.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private User validateAndBuildUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() && phone.isEmpty()) {
            etEmail.setError("Please provide an email OR phone number");
            etPhone.setError("Please provide an email OR phone number");
            return null;
        }

        if (!email.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Invalid email format");
            return null;
        }

        if (!phone.isEmpty() && phone.length() < 7) {
            etPhone.setError("Invalid phone number");
            return null;
        }

        if (name.isEmpty()) {
            etName.setError("Please provide a name");
            return null;
        }
        if (password.isEmpty()) {
            etPassword.setError("Please provide a password");
            return null;
        }

        return new User(name, email, phone, password, "CUSTOMER");
    }
}