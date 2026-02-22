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

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        ServiceResult<User> result =
                registrationService.register(name, email, phone, password);

        if (!result.success) {
            showError(result);
            return;
        }

        Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show();
        clearForm();
    }

    private void showError(ServiceResult<User> result) {

        switch (result.field) {
            case "name":
                etName.setError(result.message);
                break;
            case "email":
                etEmail.setError(result.message);
                break;
            case "phone":
                etPhone.setError(result.message);
                break;
            case "password":
                etPassword.setError(result.message);
                break;
            case "email_phone":
                etEmail.setError(result.message);
                etPhone.setError(result.message);
                break;
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