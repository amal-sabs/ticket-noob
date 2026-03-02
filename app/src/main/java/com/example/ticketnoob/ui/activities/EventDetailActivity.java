package com.example.ticketnoob.ui.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ticketnoob.R;
import com.example.ticketnoob.model.Event;
import com.example.ticketnoob.repository.EventRepository;
import com.example.ticketnoob.service.EventService;

import java.util.Locale;

public class EventDetailActivity extends AppCompatActivity {

    private EventService eventService;

    private TextView tvTitle, tvDate, tvLocation, tvCategory, tvPrice, tvSeats;

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

        eventService = new EventService(new EventRepository());

        String eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing eventId", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadEvent(eventId);
    }

    private void loadEvent(String eventId) {
        eventService.getEventById(eventId, result -> {
            if (result == null || !result.success || result.data == null) {
                Toast.makeText(this,
                        (result != null && result.message != null) ? result.message : "Failed to load event",
                        Toast.LENGTH_LONG).show();
                return;
            }

            bind(result.data);
        });
    }

    private void bind(Event event) {
        tvTitle.setText(event.getTitle());
        tvDate.setText(event.getDate());
        tvLocation.setText(event.getLocation());
        tvCategory.setText(event.getCategory());
        tvPrice.setText(String.format(Locale.US, "$%.2f", event.getPrice()));
        tvSeats.setText(event.getAvailableSeats() + " seats left");
    }
}