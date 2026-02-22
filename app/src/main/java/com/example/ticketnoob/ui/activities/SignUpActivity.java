package com.example.ticketnoob.ui.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ticketnoob.R;
import com.example.ticketnoob.model.User;
import com.example.ticketnoob.repository.UserRepository;
import com.example.ticketnoob.service.RegistrationService;
import com.example.ticketnoob.service.ServiceResult;

public class SignUpActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone, etPassword;
    private Button btnRegister;
    private RegistrationService registrationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);

        UserRepository userRepository = new UserRepository();
        registrationService = new RegistrationService(userRepository);

        btnRegister.setOnClickListener(v -> handleRegistration());
    }

    private void handleRegistration() {

        etName.setError(null);
        etEmail.setError(null);
        etPhone.setError(null);
        etPassword.setError(null);

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        setLoading(true);

        registrationService.register(name, email, phone, password, result -> {
            setLoading(false);

            if (result == null) {
                Toast.makeText(this, "Registration failed (no response)", Toast.LENGTH_LONG).show();
                return;
            }

            if (!result.success) {
                showError(result);
                return;
            }

            Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show();
            clearForm();
        });
    }

    private void setLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
    }

    private void showError(ServiceResult<User> result) {

        switch (result.field) {
            case "name":
                etName.setError(result.message);
                etName.requestFocus();
                break;
            case "email":
                etEmail.setError(result.message);
                etEmail.requestFocus();
                break;
            case "phone":
                etPhone.setError(result.message);
                etPhone.requestFocus();
                break;
            case "password":
                etPassword.setError(result.message);
                etPassword.requestFocus();
                break;
            case "email_phone":
                etEmail.setError(result.message);
                etPhone.setError(result.message);
                etEmail.requestFocus();
                break;
            case "repository":
            default:
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show();
        }
    }

    private void clearForm() {
        etName.setText("");
        etEmail.setText("");
        etPhone.setText("");
        etPassword.setText("");
    }
}