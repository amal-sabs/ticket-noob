package com.example.ticketnoob.ui.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ticketnoob.R;
import com.example.ticketnoob.repository.UserRepository;
import com.example.ticketnoob.service.LoginService;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmailOrPhone;
    private EditText etPassword;
    private Button btnLogin;

    private LoginService loginService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmailOrPhone = findViewById(R.id.etEmailOrPhone);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        UserRepository userRepository = new UserRepository();

        loginService = new LoginService(this, userRepository, etEmailOrPhone, etPassword, btnLogin);
        loginService.init();
    }
}