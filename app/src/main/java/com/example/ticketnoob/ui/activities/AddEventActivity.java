package com.example.ticketnoob.ui.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ticketnoob.R;
import com.example.ticketnoob.repository.EventRepository;
import com.example.ticketnoob.repository.ReservationRepository;
import com.example.ticketnoob.repository.UserRepository;
import com.example.ticketnoob.service.AdminEventService;

public class AddEventActivity extends AppCompatActivity {

    private EditText etTitle, etDescription, etDate, etLocation, etCategory, etCapacity, etPrice;
    private Button btnCreate;
    private AdminEventService adminEventService;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        etTitle = findViewById(R.id.etAddTitle);
        etDescription = findViewById(R.id.etAddDescription);
        etDate = findViewById(R.id.etAddDate);
        etLocation = findViewById(R.id.etAddLocation);
        etCategory = findViewById(R.id.etAddCategory);
        etCapacity = findViewById(R.id.etAddCapacity);
        etPrice = findViewById(R.id.etAddPrice);
        btnCreate = findViewById(R.id.btnCreateEvent);

        adminEventService = new AdminEventService(new EventRepository(), new ReservationRepository());

        currentUserId = getIntent().getStringExtra("userId");
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Missing userId", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnCreate.setOnClickListener(v -> createEvent());
    }

    private void createEvent() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String capacityStr = etCapacity.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Title required");
            etTitle.requestFocus();
            return;
        }
        if (description.isEmpty()) {
            etDescription.setError("Description required");
            etDescription.requestFocus();
            return;
        }
        if (date.isEmpty()) {
            etDate.setError("Date required");
            etDate.requestFocus();
            return;
        }
        if (location.isEmpty()) {
            etLocation.setError("Location required");
            etLocation.requestFocus();
            return;
        }
        if (category.isEmpty()) {
            etCategory.setError("Category required");
            etCategory.requestFocus();
            return;
        }
        if (capacityStr.isEmpty()) {
            etCapacity.setError("Capacity required");
            etCapacity.requestFocus();
            return;
        }
        if (priceStr.isEmpty()) {
            etPrice.setError("Price required");
            etPrice.requestFocus();
            return;
        }

        int capacity;
        double price;
        try {
            capacity = Integer.parseInt(capacityStr);
        } catch (NumberFormatException e) {
            etCapacity.setError("Invalid number");
            etCapacity.requestFocus();
            return;
        }
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            etPrice.setError("Invalid price");
            etPrice.requestFocus();
            return;
        }

        btnCreate.setEnabled(false);
        btnCreate.setText("Creating...");

        UserRepository userRepository = new UserRepository();
        userRepository.findById(currentUserId, (user, error) -> {
            if (user == null) {
                Toast.makeText(this,
                        error != null ? error : "Failed to load user",
                        Toast.LENGTH_LONG).show();
                btnCreate.setEnabled(true);
                btnCreate.setText("Create Event");
                return;
            }

            adminEventService.createEvent(
                    user,
                    title,
                    description,
                    date,
                    location,
                    category,
                    capacity,
                    price,
                    result -> {
                        if (result == null || !result.success) {
                            Toast.makeText(this,
                                    (result != null && result.message != null) ? result.message : "Failed to create event",
                                    Toast.LENGTH_LONG).show();
                            btnCreate.setEnabled(true);
                            btnCreate.setText("Create Event");
                            return;
                        }

                        Toast.makeText(this, "Event created!", Toast.LENGTH_SHORT).show();
                        finish(); // Back to event list
                    }
            );
        });
    }
}