package com.example.ticketnoob.ui.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ticketnoob.R;
import com.example.ticketnoob.model.User;
import com.example.ticketnoob.repository.UserRepository;
import com.example.ticketnoob.service.LoginService;
import com.example.ticketnoob.service.ServiceResult;
import com.example.ticketnoob.ui.activities.EventListActivity;

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

        String prefill = getIntent().getStringExtra("emailOrPhone");
        if (prefill != null && !prefill.isEmpty()){
            etEmailOrPhone.setText(prefill);
        }
        UserRepository userRepository = new UserRepository();
        loginService = new LoginService(userRepository);

        btnLogin.setOnClickListener(v -> handleLogin());
    }

    private void handleLogin() {

        etEmailOrPhone.setError(null);
        etPassword.setError(null);

        String emailOrPhone = etEmailOrPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        setLoading(true);

        loginService.login(emailOrPhone, password, result -> {
            setLoading(false);

            if (!result.success) {
                showError(result);
                return;
            }
            // SUCCESS:
            Intent intent = new Intent(LoginActivity.this, EventListActivity.class);
            User user = result.data;
            if(user != null){
                intent.putExtra("userName", user.getName());
                intent.putExtra("userId", user.getId());
                intent.putExtra("userRole", user.getRole());
            }

            startActivity(intent);

            // close loginActivity so pressing back doesn't return to it
            finish();
        });
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Logging in..." : "Login");
    }

    private void showError(ServiceResult<?> result) {
        if (result == null) {
            Toast.makeText(this, "Unknown error", Toast.LENGTH_SHORT).show();
            return;
        }

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
            case "repository":
            default:
                Toast.makeText(this,
                        result.message != null ? result.message : "Login failed",
                        Toast.LENGTH_SHORT).show();
                break;
        }
    }
}