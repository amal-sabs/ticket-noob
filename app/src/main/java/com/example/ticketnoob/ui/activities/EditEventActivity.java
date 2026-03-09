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

import java.util.Locale;

public class EditEventActivity extends AppCompatActivity {

    private EditText etDate, etDescription, etCapacity, etPrice;
    private Button btnSave;
    private AdminEventService adminEventService;

    private String eventId;
    private String eventTitle;
    private String eventLocation;
    private String eventCategory;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);

        etDate = findViewById(R.id.etEditDate);
        etDescription = findViewById(R.id.etEditDescription);
        etCapacity = findViewById(R.id.etEditCapacity);
        etPrice = findViewById(R.id.etEditPrice);
        btnSave = findViewById(R.id.btnSaveEvent);

        adminEventService = new AdminEventService(new EventRepository(), new ReservationRepository());

        // Read all the current values from the intent
        eventId = getIntent().getStringExtra("eventId");
        eventTitle = getIntent().getStringExtra("eventTitle");
        eventLocation = getIntent().getStringExtra("eventLocation");
        eventCategory = getIntent().getStringExtra("eventCategory");
        currentUserId = getIntent().getStringExtra("userId");

        String eventDescription = getIntent().getStringExtra("eventDescription");
        String eventDate = getIntent().getStringExtra("eventDate");
        int eventCapacity = getIntent().getIntExtra("eventCapacity", 0);
        double eventPrice = getIntent().getDoubleExtra("eventPrice", 0.0);

        // Pre-populate the form
        etDate.setText(eventDate);
        etDescription.setText(eventDescription);
        etCapacity.setText(String.valueOf(eventCapacity));
        etPrice.setText(String.format(Locale.US, "%.2f", eventPrice));

        btnSave.setOnClickListener(v -> saveEvent());
    }

    private void saveEvent() {
        String date = etDate.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String capacityStr = etCapacity.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();

        // Basic validation
        if (date.isEmpty()) {
            etDate.setError("Date required");
            etDate.requestFocus();
            return;
        }
        if (description.isEmpty()) {
            etDescription.setError("Description required");
            etDescription.requestFocus();
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

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        // Fetch the admin user from Firestore
        UserRepository userRepository = new UserRepository();
        userRepository.findById(currentUserId, (user, error) -> {
            if (user == null) {
                Toast.makeText(this,
                        error != null ? error : "Failed to load user",
                        Toast.LENGTH_LONG).show();
                btnSave.setEnabled(true);
                btnSave.setText("Save Changes");
                return;
            }

            adminEventService.updateEvent(
                    user,
                    eventId,
                    eventTitle,
                    description,
                    date,
                    eventLocation,
                    eventCategory,
                    capacity,
                    price,
                    result -> {
                        if (result == null || !result.success) {
                            Toast.makeText(this,
                                    (result != null && result.message != null) ? result.message : "Failed to update event",
                                    Toast.LENGTH_LONG).show();
                            btnSave.setEnabled(true);
                            btnSave.setText("Save Changes");
                            return;
                        }

                        Toast.makeText(this, "Event updated!", Toast.LENGTH_SHORT).show();
                        finish(); // Go back to event list
                    }
            );
        });
    }
}