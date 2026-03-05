package com.example.ticketnoob.ui.activities;

import android.os.Bundle;
import android.widget.Toast;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
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
    private String selectedDate = "";
    private String selectedLocation = "";
    private String selectedCategory = "";

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
        findViewById(R.id.btnFilter).setOnClickListener(v -> showFilterDialog());

        eventService = new EventService(new EventRepository());

//        new EventRepository().seedSampleEvents(); // only keep temporarily
        applyFilters();
    }

    private void applyFilters() {
        eventService.filterEvents(selectedDate, selectedLocation, selectedCategory, true, result -> {
            if (result == null || !result.success) {
                String msg = (result != null && result.message != null)
                        ? result.message
                        : "Failed to load events";
                Toast.makeText(EventListActivity.this, msg, Toast.LENGTH_LONG).show();
                return;
            }

            List<Event> newEvents = (result.data != null) ? result.data : new ArrayList<>();
            eventAdapter.updateEvents(newEvents);

            Toast.makeText(
                    this,
                    getString(R.string.filter_result_count, newEvents.size()),
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    private void showFilterDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_event_filters, null);

        EditText etFilterDate = dialogView.findViewById(R.id.etFilterDate);
        EditText etFilterLocation = dialogView.findViewById(R.id.etFilterLocation);
        EditText etFilterCategory = dialogView.findViewById(R.id.etFilterCategory);

        etFilterDate.setText(selectedDate);
        etFilterLocation.setText(selectedLocation);
        etFilterCategory.setText(selectedCategory);

        new AlertDialog.Builder(this)
                .setTitle(R.string.filter_title)
                .setView(dialogView)
                .setNegativeButton(R.string.filter_cancel, null)
                .setNeutralButton(R.string.filter_clear, (dialog, which) -> {
                    selectedDate = "";
                    selectedLocation = "";
                    selectedCategory = "";
                    applyFilters();
                })
                .setPositiveButton(R.string.filter_apply, (dialog, which) -> {
                    selectedDate = safeTrim(etFilterDate.getText() != null ? etFilterDate.getText().toString() : null);
                    selectedLocation = safeTrim(etFilterLocation.getText() != null ? etFilterLocation.getText().toString() : null);
                    selectedCategory = safeTrim(etFilterCategory.getText() != null ? etFilterCategory.getText().toString() : null);
                    applyFilters();
                })
                .show();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}