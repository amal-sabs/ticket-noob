package com.example.ticketnoob.ui.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ticketnoob.R;
import com.example.ticketnoob.model.User;
import com.example.ticketnoob.repository.UserRepository;
import com.example.ticketnoob.service.LoginService;
import com.example.ticketnoob.service.ServiceResult;

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
        loginService = new LoginService(userRepository);

        btnLogin.setOnClickListener(v -> handleLogin());
    }

    private void handleLogin() {

        String emailOrPhone = etEmailOrPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        ServiceResult<User> result =
                loginService.login(emailOrPhone, password);

        if (!result.success) {
            showError(result);
            return;
        }

        User user = result.data;
        Toast.makeText(this,
                "Welcome " + user.getName(),
                Toast.LENGTH_SHORT).show();
    }

    private void showError(ServiceResult<?> result) {

        switch (result.field) {
            case "emailOrPhone":
                etEmailOrPhone.setError(result.message);
                etEmailOrPhone.requestFocus();
                break;

            case "password":
                etPassword.setError(result.message);
                etPassword.requestFocus();
                break;

            case "credentials":
                Toast.makeText(this,
                        result.message,
                        Toast.LENGTH_SHORT).show();
                break;
        }
    }
}