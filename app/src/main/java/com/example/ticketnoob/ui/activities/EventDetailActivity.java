package com.example.ticketnoob.ui.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ticketnoob.R;
import com.example.ticketnoob.model.Event;
import com.example.ticketnoob.repository.EventRepository;
import com.example.ticketnoob.repository.ReservationRepository;
import com.example.ticketnoob.service.EventService;
import com.example.ticketnoob.service.ReservationService;

import java.util.Locale;

public class EventDetailActivity extends AppCompatActivity {

    private EventService eventService;
    private ReservationService reservationService;

    private TextView tvTitle, tvDate, tvLocation, tvCategory, tvPrice, tvSeats;
    private Button btnBook;
    private String currentUserId;
    private Event currentEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        tvTitle = findViewById(R.id.tvEventTitle);
        tvDate = findViewById(R.id.tvEventDate);
        tvLocation = findViewById(R.id.tvEventLocation);
        tvCategory = findViewById(R.id.tvEventCategory);
        tvPrice = findViewById(R.id.tvEventPrice);
        tvSeats = findViewById(R.id.tvEventSeats);
        btnBook = findViewById(R.id.btnBook);

        eventService = new EventService(new EventRepository());
        reservationService = new ReservationService(new ReservationRepository(), new EventRepository());

        String eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing eventId", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        currentUserId = getIntent().getStringExtra("userId");
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Missing userId", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnBook.setOnClickListener(v -> bookEvent());

        loadEvent(eventId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentEvent != null) {
            loadEvent(currentEvent.getId());
        }
    }

    private void loadEvent(String eventId) {
        eventService.getEventById(eventId, result -> {
            if (result == null || !result.success || result.data == null) {
                Toast.makeText(this,
                        (result != null && result.message != null) ? result.message : "Failed to load event",
                        Toast.LENGTH_LONG).show();
                return;
            }

            currentEvent = result.data;
            bind(currentEvent);
            checkExistingReservation(eventId);
        });
    }

    private void bind(Event event) {
        tvTitle.setText(event.getTitle());
        tvDate.setText(event.getDate());
        tvLocation.setText(event.getLocation());
        tvCategory.setText(event.getCategory());
        tvPrice.setText(String.format(Locale.US, "$%.2f", event.getPrice()));
        tvSeats.setText(event.getAvailableSeats() + " seats left");

        // Disable booking if no seats available
        if (event.getAvailableSeats() <= 0) {
            btnBook.setEnabled(false);
            btnBook.setText("Sold Out");
        }
    }

    private void bookEvent() {
        if (currentEvent == null) return;

        String userId = currentUserId;

        btnBook.setEnabled(false);
        btnBook.setText("Booking...");

        reservationService.createReservation(userId, currentEvent.getId(), result -> {
            if (result == null || !result.success) {
                Toast.makeText(this,
                        (result != null && result.message != null) ? result.message : "Reservation failed",
                        Toast.LENGTH_LONG).show();
                btnBook.setEnabled(true);
                btnBook.setText("Book");
                return;
            }

            Toast.makeText(this, "Reservation confirmed!", Toast.LENGTH_SHORT).show();
            btnBook.setEnabled(false);
            btnBook.setText("Booked");
            loadEvent(currentEvent.getId());
        });
    }

    private void checkExistingReservation(String eventId) {
        String userId = currentUserId;

        reservationService.getActiveReservationsForUser(userId, result -> {
            if (result == null || !result.success || result.data == null) return;

            boolean alreadyBooked = result.data.stream()
                    .anyMatch(r -> r.getEventId().equals(eventId));

            if (alreadyBooked) {
                btnBook.setEnabled(false);
                btnBook.setText("Booked");
            }
        });
    }
}