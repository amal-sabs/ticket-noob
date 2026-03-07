package com.example.ticketnoob.ui.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketnoob.R;
import com.example.ticketnoob.model.Event;
import com.example.ticketnoob.model.Reservation;
import com.example.ticketnoob.repository.EventRepository;
import com.example.ticketnoob.repository.ReservationRepository;
import com.example.ticketnoob.service.EventService;
import com.example.ticketnoob.service.ReservationService;
import com.example.ticketnoob.util.NavHelper;
import com.example.ticketnoob.util.ReservationsAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MyReservationsActivity extends AppCompatActivity {

    private ReservationsAdapter reservationsAdapter;
    private final List<Reservation> reservations = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();

    private ReservationService reservationService;
    private EventService eventService;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reservations);

        currentUserId = getIntent().getStringExtra("userId");
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Missing userId", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        RecyclerView rvReservations = findViewById(R.id.rvReservations);
        rvReservations.setLayoutManager(new LinearLayoutManager(this));

        reservationsAdapter = new ReservationsAdapter(reservations, events, this::cancelReservation);
        rvReservations.setAdapter(reservationsAdapter);

        reservationService = new ReservationService(new ReservationRepository(), new EventRepository());
        eventService = new EventService(new EventRepository());

        loadTickets();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        NavHelper.setup(this, bottomNav, R.id.nav_reservations, currentUserId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.getMenu().findItem(R.id.nav_reservations).setChecked(true);
    }

    private void loadTickets() {
        reservationService.getActiveReservationsForUser(currentUserId, result -> {
            if (result == null || !result.success || result.data == null) {
                Toast.makeText(this,
                        (result != null && result.message != null) ? result.message : "Failed to load tickets",
                        Toast.LENGTH_LONG).show();
                return;
            }

            List<Reservation> userReservations = result.data;
            if (userReservations.isEmpty()) {
                Toast.makeText(this, "No active tickets", Toast.LENGTH_SHORT).show();
                //ticketAdapter.updateData(new ArrayList<>(), new ArrayList<>());
                return;
            }

            // Fetch the event for each reservation
            List<Event> fetchedEvents = new ArrayList<>();
            for (int i = 0; i < userReservations.size(); i++) {
                fetchedEvents.add(null); // placeholder to maintain index order
            }

            int[] completedCount = {0};
            for (int i = 0; i < userReservations.size(); i++) {
                final int index = i;
                eventService.getEventById(userReservations.get(index).getEventId(), eventResult -> {
                    if (eventResult != null && eventResult.success && eventResult.data != null) {
                        fetchedEvents.set(index, eventResult.data);
                    }
                    completedCount[0]++;
                    if (completedCount[0] == userReservations.size()) {
                        // Filter out any reservations where event failed to load
                        List<Reservation> validReservations = new ArrayList<>();
                        List<Event> validEvents = new ArrayList<>();
                        for (int j = 0; j < userReservations.size(); j++) {
                            if (fetchedEvents.get(j) != null) {
                                validReservations.add(userReservations.get(j));
                                validEvents.add(fetchedEvents.get(j));
                            }
                        }
                        reservationsAdapter.updateData(validReservations, validEvents);
                    }
                });
            }
        });
    }

    private void cancelReservation(Reservation reservation) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Ticket")
                .setMessage("Are you sure you want to cancel this reservation?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", (dialog, which) -> {
                    reservationService.cancelReservation(reservation.getId(), result -> {
                        if (result == null || !result.success) {
                            Toast.makeText(this,
                                    (result != null && result.message != null) ? result.message : "Failed to cancel",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        Toast.makeText(this, "Reservation cancelled", Toast.LENGTH_SHORT).show();
                        loadTickets();
                    });
                })
                .show();
    }
}
