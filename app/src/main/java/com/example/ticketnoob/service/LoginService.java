package com.example.ticketnoob.service;

import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.ticketnoob.model.User;
import com.example.ticketnoob.repository.UserRepository;

public class LoginService {

    private final EditText etEmailOrPhone, etPassword;
    private final Button btnLogin;
    private final UserRepository userRepository;
    private final Context context;

    public LoginService(Context context,
                        UserRepository repo,
                        EditText etEmailOrPhone,
                        EditText etPassword,
                        Button btnLogin) {
        this.context = context;
        this.userRepository = repo;
        this.etEmailOrPhone = etEmailOrPhone;
        this.etPassword = etPassword;
        this.btnLogin = btnLogin;
    }

    public void init() {
        btnLogin.setOnClickListener(v -> handleLogin());
    }

    private void handleLogin() {
        String emailOrPhone = etEmailOrPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (emailOrPhone.isEmpty()) {
            etEmailOrPhone.setError("Please provide email or phone number");
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Please provide a password");
            return;
        }

        User user = userRepository.authenticate(emailOrPhone, password);

        if (user == null) {
            Toast.makeText(context, "Invalid credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(context, "Welcome " + user.getName(), Toast.LENGTH_SHORT).show();

    }
}
