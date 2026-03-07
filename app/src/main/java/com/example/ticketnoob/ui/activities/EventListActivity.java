package com.example.ticketnoob.ui.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Toast;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketnoob.R;
import com.example.ticketnoob.model.Event;
import com.example.ticketnoob.repository.EventRepository;
import com.example.ticketnoob.service.EventService;
import com.example.ticketnoob.ui.adapters.EventAdapter;
import com.example.ticketnoob.util.NavHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class EventListActivity extends AppCompatActivity {

    private RecyclerView rvEvents;
    private EventAdapter eventAdapter;

    private final List<Event> events = new ArrayList<>();

    private EventService eventService;
    private String selectedDate = "";
    private String selectedLocation = "";
    private String selectedCategory = "";
    private String selectedKeyword = "";
    private final List<Event> serviceFilteredEvents = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);

        String userId = getIntent().getStringExtra("userId");

        rvEvents = findViewById(R.id.rvEvents);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));

        eventAdapter = new EventAdapter(events, event -> {
            Intent intent = new Intent(EventListActivity.this, EventDetailActivity.class);
            intent.putExtra("eventId", event.getId());
            intent.putExtra("userId", getIntent().getStringExtra("userId"));
            startActivity(intent);
        });
        rvEvents.setAdapter(eventAdapter);
        findViewById(R.id.btnFilter).setOnClickListener(v -> showFilterDialog());
        setupSearch();

        eventService = new EventService(new EventRepository());

//        new EventRepository().seedSampleEvents(); // only keep temporarily
        applyFilters();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        NavHelper.setup(this, bottomNav, R.id.nav_events, userId);
    }

    @SuppressLint("StringFormatInvalid")
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
            serviceFilteredEvents.clear();
            serviceFilteredEvents.addAll(newEvents);
            int visibleCount = applyKeywordFilter();

            Toast.makeText(
                    this,
                    getString(R.string.filter_result_count, visibleCount),
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    private void setupSearch() {
        SearchView searchView = findViewById(R.id.svEventSearch);
        searchView.setQueryHint(getString(R.string.search_events_hint));
        searchView.setIconifiedByDefault(false);
        searchView.setIconified(false);
        searchView.clearFocus();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                selectedKeyword = safeTrim(query);
                applyKeywordFilter();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                selectedKeyword = safeTrim(newText);
                applyKeywordFilter();
                return true;
            }
        });
    }

    private int applyKeywordFilter() {
        if (selectedKeyword.isEmpty()) {
            eventAdapter.updateEvents(new ArrayList<>(serviceFilteredEvents));
            return serviceFilteredEvents.size();
        }

        List<Event> keywordFiltered = new ArrayList<>();
        String keywordLower = selectedKeyword.toLowerCase(Locale.US);

        for (Event event : serviceFilteredEvents) {
            String title = event.getTitle();
            if (title != null && title.toLowerCase(Locale.US).contains(keywordLower)) {
                keywordFiltered.add(event);
            }
        }

        eventAdapter.updateEvents(keywordFiltered);
        return keywordFiltered.size();
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