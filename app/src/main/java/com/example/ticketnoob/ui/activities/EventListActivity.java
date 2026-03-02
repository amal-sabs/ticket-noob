package com.example.ticketnoob.ui.activities;

import android.os.Bundle;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketnoob.R;
import com.example.ticketnoob.model.Event;
import com.example.ticketnoob.repository.EventRepository;
import com.example.ticketnoob.service.EventService;
import com.example.ticketnoob.ui.adapters.EventAdapter;

import java.util.ArrayList;
import java.util.List;


public class EventListActivity extends AppCompatActivity {

    private RecyclerView rvEvents;
    private EventAdapter eventAdapter;

    private final List<Event> events = new ArrayList<>();

    private EventService eventService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);

        rvEvents = findViewById(R.id.rvEvents);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));

        eventAdapter = new EventAdapter(events, event -> {
            Intent intent = new Intent(EventListActivity.this, EventDetailActivity.class);
            intent.putExtra("eventId", event.getId());

            startActivity(intent);
        });
        rvEvents.setAdapter(eventAdapter);

        eventService = new EventService(new EventRepository());

//        new EventRepository().seedSampleEvents(); // only keep temporarily
        loadEvents();
    }

    private void loadEvents() {
        eventService.getAvailableEvents(result -> {
            if (result == null || !result.success) {
                String msg = (result != null && result.message != null)
                        ? result.message
                        : "Failed to load events";
                Toast.makeText(EventListActivity.this, msg, Toast.LENGTH_LONG).show();
                return;
            }

            List<Event> newEvents = (result.data != null) ? result.data : new ArrayList<>();
            eventAdapter.updateEvents(newEvents);

            Toast.makeText(this, "Loaded events: " + newEvents.size(), Toast.LENGTH_SHORT).show();
        });
    }
}