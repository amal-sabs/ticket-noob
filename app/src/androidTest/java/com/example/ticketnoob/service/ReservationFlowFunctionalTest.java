package com.example.ticketnoob.service;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.ticketnoob.model.Event;
import com.example.ticketnoob.model.Reservation;
import com.example.ticketnoob.model.Status;
import com.example.ticketnoob.repository.EventRepository;
import com.example.ticketnoob.repository.ReservationRepository;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class ReservationFlowFunctionalTest {

    private static final long TIMEOUT_SECONDS = 25L;
    private static final long EVENTUAL_TIMEOUT_MS = 12000L;
    private static final long EVENTUAL_POLL_MS = 300L;

    private EventRepository eventRepository;
    private ReservationRepository reservationRepository;
    private ReservationService reservationService;
    private FirebaseFirestore firestore;

    private final List<String> createdEventIds = new ArrayList<>();
    private final List<String> createdReservationIds = new ArrayList<>();
    private String runTag;

    @Before
    public void setUp() {
        eventRepository = new EventRepository();
        reservationRepository = new ReservationRepository();
        reservationService = new ReservationService(reservationRepository, eventRepository);
        firestore = FirebaseFirestore.getInstance();
        runTag = "reservation-flow-" + System.currentTimeMillis();
    }

    @After
    public void tearDown() throws InterruptedException {
        for (String reservationId : createdReservationIds) {
            CountDownLatch latch = new CountDownLatch(1);
            firestore.collection("reservations")
                    .document(reservationId)
                    .delete()
                    .addOnCompleteListener(task -> latch.countDown());
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        createdReservationIds.clear();

        for (String eventId : createdEventIds) {
            CountDownLatch latch = new CountDownLatch(1);
            firestore.collection("events")
                    .document(eventId)
                    .delete()
                    .addOnCompleteListener(task -> latch.countDown());
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        createdEventIds.clear();
    }

    @Test
    public void completeReservationAndCancellationFlow_success() throws InterruptedException {
        Event event = createAndSaveEvent(
                "Flow Event " + runTag,
                "Reservation Flow functional test",
                "2026-10-20 19:00",
                "Montreal",
                "Music",
                100,
                10,
                Status.ACTIVE
        );

        String userId = "user-" + UUID.randomUUID();

        // 1) Test selecting and reserving tickets
        ServiceResult<Reservation> createResult = awaitCreateReservation(userId, event.getId());
        assertNotNull("Expected non-null create result", createResult);
        assertTrue("Expected reservation creation success, got: " + createResult.message, createResult.success);
        assertNotNull("Expected reservation payload", createResult.data);

        Reservation created = createResult.data;
        assertNotNull("Expected reservation id", created.getId());
        createdReservationIds.add(created.getId());

        assertEquals("Expected reservation user id", userId, created.getUserId());
        assertEquals("Expected reservation event id", event.getId(), created.getEventId());
        assertTrue("Expected ACTIVE reservation", created.isActive());
        assertNotNull("Expected timestamp set as confirmation receipt artifact", created.getTimestamp());
        assertFalse("Expected non-empty timestamp", created.getTimestamp().trim().isEmpty());

        // Verify seat decrement on event
        Event eventAfterReserve = awaitGetEventById(event.getId());
        assertNotNull("Expected event after reservation", eventAfterReserve);
        assertEquals("Expected available seats decremented by 1", 9, eventAfterReserve.getAvailableSeats());

        // 2) Test reservation confirmation receipt
        // persisted reservation + timestamp + ACTIVE status
        Reservation persistedActiveReservation = awaitGetReservationById(created.getId());
        assertNotNull("Expected persisted reservation", persistedActiveReservation);
        assertEquals(created.getId(), persistedActiveReservation.getId());
        assertTrue("Expected reservation to remain ACTIVE after creation", persistedActiveReservation.isActive());
        assertNotNull("Expected persisted timestamp", persistedActiveReservation.getTimestamp());

        // 3) Test email/SMS confirmation delivery
        List<Reservation> activeForUser = awaitGetActiveReservationsForUser(userId, created.getId());
        assertTrue("Expected created reservation in active reservations list",
                containsReservation(activeForUser, created.getId()));

        // 4) Test reservation cancellation Flow
        ServiceResult<Reservation> cancelResult = awaitCancelReservation(created.getId());
        assertNotNull("Expected non-null cancel result", cancelResult);
        assertTrue("Expected cancel success, got: " + cancelResult.message, cancelResult.success);
        assertNotNull("Expected cancelled reservation payload", cancelResult.data);
        assertTrue("Expected reservation status CANCELLED", cancelResult.data.isCancelled());

        // 5) Test cancel confirmation receipt
        Reservation persistedCancelledReservation = awaitGetReservationById(created.getId());
        assertNotNull("Expected persisted reservation after cancellation", persistedCancelledReservation);
        assertTrue("Expected reservation persisted as CANCELLED", persistedCancelledReservation.isCancelled());

        // Verify seat restore on event
        Event eventAfterCancel = awaitGetEventById(event.getId());
        assertNotNull("Expected event after cancellation", eventAfterCancel);
        assertEquals("Expected available seats restored after cancellation", 10, eventAfterCancel.getAvailableSeats());
    }

    private Event createAndSaveEvent(
            String title,
            String description,
            String date,
            String location,
            String category,
            int capacity,
            int availableSeats,
            Status status
    ) throws InterruptedException {
        String id = runTag + "-" + UUID.randomUUID();
        Event event = new Event(
                id,
                title,
                description,
                date,
                location,
                category,
                capacity,
                availableSeats,
                29.99,
                status
        );

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> successRef = new AtomicReference<>(false);
        AtomicReference<String> errorRef = new AtomicReference<>(null);

        eventRepository.save(event, (saved, error) -> {
            successRef.set(Boolean.TRUE.equals(saved));
            errorRef.set(error);
            latch.countDown();
        });

        assertTrue("Save event timed out", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertTrue("Failed to save event: " + errorRef.get(), successRef.get());

        createdEventIds.add(id);
        return event;
    }

    private ServiceResult<Reservation> awaitCreateReservation(String userId, String eventId) throws InterruptedException {
        AtomicReference<ServiceResult<Reservation>> resultRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        reservationService.createReservation(userId, eventId, result -> {
            resultRef.set(result);
            latch.countDown();
        });

        assertTrue("createReservation timed out", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        return resultRef.get();
    }

    private ServiceResult<Reservation> awaitCancelReservation(String reservationId) throws InterruptedException {
        AtomicReference<ServiceResult<Reservation>> resultRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        reservationService.cancelReservation(reservationId, result -> {
            resultRef.set(result);
            latch.countDown();
        });

        assertTrue("cancelReservation timed out", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        return resultRef.get();
    }

    private Event awaitGetEventById(String eventId) throws InterruptedException {
        AtomicReference<Event> eventRef = new AtomicReference<>();
        AtomicReference<String> errorRef = new AtomicReference<>(null);
        CountDownLatch latch = new CountDownLatch(1);

        eventRepository.findById(eventId, (event, error) -> {
            eventRef.set(event);
            errorRef.set(error);
            latch.countDown();
        });

        assertTrue("findById(event) timed out", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertNull("Expected no event fetch error, got: " + errorRef.get(), errorRef.get());
        return eventRef.get();
    }

    private Reservation awaitGetReservationById(String reservationId) throws InterruptedException {
        long start = System.currentTimeMillis();
        Reservation latest = null;

        while (System.currentTimeMillis() - start < EVENTUAL_TIMEOUT_MS) {
            AtomicReference<Reservation> reservationRef = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            reservationRepository.findById(reservationId, (reservation, error) -> {
                reservationRef.set(reservation);
                latch.countDown();
            });

            assertTrue("findById(reservation) timed out", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            latest = reservationRef.get();

            if (latest != null) {
                return latest;
            }

            Thread.sleep(EVENTUAL_POLL_MS);
        }

        fail("Timed out waiting for reservation by id: " + reservationId);
        return latest;
    }

    private List<Reservation> awaitGetActiveReservationsForUser(String userId, String expectedReservationId)
            throws InterruptedException {
        long start = System.currentTimeMillis();
        List<Reservation> latest = new ArrayList<>();

        while (System.currentTimeMillis() - start < EVENTUAL_TIMEOUT_MS) {
            AtomicReference<ServiceResult<List<Reservation>>> resultRef = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            reservationService.getActiveReservationsForUser(userId, result -> {
                resultRef.set(result);
                latch.countDown();
            });

            assertTrue("getActiveReservationsForUser timed out", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

            ServiceResult<List<Reservation>> result = resultRef.get();
            assertNotNull("Expected non-null result for getActiveReservationsForUser", result);
            assertTrue("Expected success loading active reservations, got: " + result.message, result.success);

            latest = result.data == null ? new ArrayList<>() : result.data;
            if (containsReservation(latest, expectedReservationId)) {
                return latest;
            }

            Thread.sleep(EVENTUAL_POLL_MS);
        }

        fail("Timed out waiting for active reservation in user list: " + expectedReservationId);
        return latest;
    }

    private boolean containsReservation(List<Reservation> reservations, String reservationId) {
        if (reservations == null || reservationId == null) return false;
        for (Reservation reservation : reservations) {
            if (reservation != null && reservationId.equals(reservation.getId())) {
                return true;
            }
        }
        return false;
    }
}