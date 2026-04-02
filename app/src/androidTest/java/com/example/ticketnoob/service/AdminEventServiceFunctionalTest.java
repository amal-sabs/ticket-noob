package com.example.ticketnoob.service;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.ticketnoob.model.Event;
import com.example.ticketnoob.model.Reservation;
import com.example.ticketnoob.model.Status;
import com.example.ticketnoob.model.User;
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
public class AdminEventServiceFunctionalTest {

    private static final long TIMEOUT_SECONDS = 25L;
    private static final long EVENTUAL_TIMEOUT_MS = 12000L;
    private static final long EVENTUAL_POLL_MS = 300L;

    private EventRepository eventRepository;
    private ReservationRepository reservationRepository;
    private AdminEventService adminEventService;
    private FirebaseFirestore firestore;

    private final List<String> createdEventIds = new ArrayList<>();
    private final List<String> createdReservationIds = new ArrayList<>();
    private String runTag;

    @Before
    public void setUp() {
        eventRepository = new EventRepository();
        reservationRepository = new ReservationRepository();
        adminEventService = new AdminEventService(eventRepository, reservationRepository);
        firestore = FirebaseFirestore.getInstance();
        runTag = "admin-func-" + System.currentTimeMillis();
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

    // 1) Test adding a new event
    @Test
    public void createEvent_validInputs_succeeds() throws InterruptedException {
        User admin = adminUser();

        ServiceResult<Event> result = awaitCreateEvent(
                admin,
                "Admin Created Event " + runTag,
                "Created by admin",
                "2026-11-01 18:00",
                "Montreal",
                "Music",
                150,
                49.99
        );

        assertNotNull(result);
        assertTrue("Expected success: " + result.message, result.success);
        assertNotNull(result.data);

        Event created = waitForCreatedEvent(result.data);
        createdEventIds.add(created.getId());

        assertEquals("Admin Created Event " + runTag, created.getTitle());
        assertEquals("Created by admin", created.getDescription());
        assertEquals("2026-11-01 18:00", created.getDate());
        assertEquals("Montreal", created.getLocation());
        assertEquals("Music", created.getCategory());
        assertEquals(150, created.getCapacity());
        assertEquals(150, created.getAvailableSeats());
        assertEquals(49.99, created.getPrice(), 0.0001);
        assertTrue(created.isActive());
    }

    // 2) Test adding event with invalid/missing data
    @Test
    public void createEvent_invalidOrMissingData_returnsValidationErrors() throws InterruptedException {
        User admin = adminUser();

        ServiceResult<Event> missingTitle = awaitCreateEvent(
                admin,
                "   ",
                "Desc",
                "2026-11-02 18:00",
                "Montreal",
                "Music",
                100,
                20.0
        );
        assertFalse(missingTitle.success);
        assertEquals("title", missingTitle.field);
        assertEquals("Title required", missingTitle.message);

        ServiceResult<Event> missingDate = awaitCreateEvent(
                admin,
                "Event " + runTag,
                "Desc",
                " ",
                "Montreal",
                "Music",
                100,
                20.0
        );
        assertFalse(missingDate.success);
        assertEquals("date", missingDate.field);

        ServiceResult<Event> invalidCapacity = awaitCreateEvent(
                admin,
                "Event " + runTag,
                "Desc",
                "2026-11-02 18:00",
                "Montreal",
                "Music",
                0,
                20.0
        );
        assertFalse(invalidCapacity.success);
        assertEquals("capacity", invalidCapacity.field);

        ServiceResult<Event> invalidPrice = awaitCreateEvent(
                admin,
                "Event " + runTag,
                "Desc",
                "2026-11-02 18:00",
                "Montreal",
                "Music",
                100,
                -0.01
        );
        assertFalse(invalidPrice.success);
        assertEquals("price", invalidPrice.field);
    }

    // 3) Test editing existing event details
    @Test
    public void updateEvent_existingEvent_updatesDetails() throws InterruptedException {
        User admin = adminUser();

        Event seed = createAndSaveEvent(
                "Original " + runTag,
                "Original desc",
                "2026-12-01 20:00",
                "Quebec City",
                "Conference",
                120,
                110,
                Status.ACTIVE
        );

        ServiceResult<Event> updateResult = awaitUpdateEvent(
                admin,
                seed.getId(),
                "Updated " + runTag,
                "Updated desc",
                "2026-12-02 21:00",
                "Montreal",
                "Workshop",
                200,
                79.99
        );

        assertNotNull(updateResult);
        assertTrue("Expected update success: " + updateResult.message, updateResult.success);
        assertNotNull(updateResult.data);

        Event updated = awaitGetEventById(seed.getId());
        assertNotNull(updated);
        assertEquals("Updated " + runTag, updated.getTitle());
        assertEquals("Updated desc", updated.getDescription());
        assertEquals("2026-12-02 21:00", updated.getDate());
        assertEquals("Montreal", updated.getLocation());
        assertEquals("Workshop", updated.getCategory());
        assertEquals(200, updated.getCapacity());

        assertEquals(190, updated.getAvailableSeats());
        assertEquals(79.99, updated.getPrice(), 0.0001);
    }

    // 4) Test canceling an event
    @Test
    public void cancelEvent_cancelsEventAndActiveReservations() throws InterruptedException {
        User admin = adminUser();

        Event event = createAndSaveEvent(
                "Cancelable " + runTag,
                "Will be cancelled",
                "2026-12-10 19:00",
                "Laval",
                "Sports",
                100,
                100,
                Status.ACTIVE
        );

        Reservation r1 = createAndSaveReservation("userA-" + runTag, event.getId(), Status.ACTIVE);
        Reservation r2 = createAndSaveReservation("userB-" + runTag, event.getId(), Status.ACTIVE);

        ServiceResult<Event> cancelResult = awaitCancelEvent(admin, event.getId());
        assertNotNull(cancelResult);
        assertTrue("Expected cancel success: " + cancelResult.message, cancelResult.success);
        assertNotNull(cancelResult.data);
        assertTrue(cancelResult.data.isCancelled());

        Event cancelledEvent = awaitGetEventById(event.getId());
        assertNotNull(cancelledEvent);
        assertTrue("Expected event status CANCELLED", cancelledEvent.isCancelled());

        Reservation rr1 = awaitGetReservationById(r1.getId());
        Reservation rr2 = awaitGetReservationById(r2.getId());

        assertNotNull(rr1);
        assertNotNull(rr2);
        assertTrue("Expected reservation 1 cancelled", rr1.isCancelled());
        assertTrue("Expected reservation 2 cancelled", rr2.isCancelled());
    }

    // 5) Test admin access to edit panel
    @Test
    public void adminPanelAccess_onlyAdminCanPerformAdminOperations() throws InterruptedException {
        User customer = customerUser();

        ServiceResult<Event> result = awaitCreateEvent(
                customer,
                "Unauthorized " + runTag,
                "Should fail",
                "2026-11-20 18:00",
                "Montreal",
                "Music",
                100,
                30.0
        );

        assertNotNull(result);
        assertFalse("Expected non-admin operation denial", result.success);
        assertEquals("role", result.field);
        assertEquals("Only admins can perform this action", result.message);
    }

    // Helpers

    private User adminUser() {
        return new User(
                "admin-" + runTag,
                "Admin User",
                "admin+" + runTag + "@test.com",
                "",
                "password123",
                "ADMIN"
        );
    }

    private User customerUser() {
        return new User(
                "customer-" + runTag,
                "Customer User",
                "customer+" + runTag + "@test.com",
                "",
                "password123",
                "CUSTOMER"
        );
    }

    private ServiceResult<Event> awaitCreateEvent(
            User user,
            String title,
            String description,
            String date,
            String location,
            String category,
            int capacity,
            double price
    ) throws InterruptedException {
        AtomicReference<ServiceResult<Event>> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        adminEventService.createEvent(
                user, title, description, date, location, category, capacity, price,
                result -> {
                    ref.set(result);
                    latch.countDown();
                }
        );

        assertTrue("createEvent timed out", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        return ref.get();
    }

    private ServiceResult<Event> awaitUpdateEvent(
            User user,
            String eventId,
            String title,
            String description,
            String date,
            String location,
            String category,
            int capacity,
            double price
    ) throws InterruptedException {
        AtomicReference<ServiceResult<Event>> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        adminEventService.updateEvent(
                user, eventId, title, description, date, location, category, capacity, price,
                result -> {
                    ref.set(result);
                    latch.countDown();
                }
        );

        assertTrue("updateEvent timed out", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        return ref.get();
    }

    private ServiceResult<Event> awaitCancelEvent(User user, String eventId) throws InterruptedException {
        AtomicReference<ServiceResult<Event>> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        adminEventService.cancelEvent(user, eventId, result -> {
            ref.set(result);
            latch.countDown();
        });

        assertTrue("cancelEvent timed out", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        return ref.get();
    }

    private Event waitForCreatedEvent(Event fromService) throws InterruptedException {
        assertNotNull("Service event must not be null", fromService);
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < EVENTUAL_TIMEOUT_MS) {
            String id = fromService.getId();
            if (id != null && !id.trim().isEmpty()) {
                Event persisted = awaitGetEventById(id);
                if (persisted != null) return persisted;
            }
            Thread.sleep(EVENTUAL_POLL_MS);
        }

        fail("Timed out waiting for created event id to be available");
        return null;
    }

    private Event awaitGetEventById(String eventId) throws InterruptedException {
        AtomicReference<Event> eventRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        eventRepository.findById(eventId, (event, error) -> {
            eventRef.set(event);
            latch.countDown();
        });

        assertTrue("findById(event) timed out", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        return eventRef.get();
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
        String id = runTag + "-event-" + UUID.randomUUID();
        Event event = new Event(
                id, title, description, date, location, category,
                capacity, availableSeats, 25.0, status
        );

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> success = new AtomicReference<>(false);
        AtomicReference<String> errorRef = new AtomicReference<>(null);

        eventRepository.save(event, (saved, error) -> {
            success.set(Boolean.TRUE.equals(saved));
            errorRef.set(error);
            latch.countDown();
        });

        assertTrue("save event timed out", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertTrue("save event failed: " + errorRef.get(), success.get());

        createdEventIds.add(id);
        return event;
    }

    private Reservation createAndSaveReservation(String userId, String eventId, Status status) throws InterruptedException {
        Reservation reservation = new Reservation(userId, eventId, String.valueOf(System.currentTimeMillis()));
        reservation.setStatusEnum(status);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> success = new AtomicReference<>(false);
        AtomicReference<String> errorRef = new AtomicReference<>(null);

        reservationRepository.save(reservation, (saved, error) -> {
            success.set(Boolean.TRUE.equals(saved));
            errorRef.set(error);
            latch.countDown();
        });

        assertTrue("save reservation timed out", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertTrue("save reservation failed: " + errorRef.get(), success.get());

        assertNotNull("reservation id expected after save", reservation.getId());
        createdReservationIds.add(reservation.getId());
        return reservation;
    }

    private Reservation awaitGetReservationById(String reservationId) throws InterruptedException {
        long start = System.currentTimeMillis();
        Reservation latest = null;

        while (System.currentTimeMillis() - start < EVENTUAL_TIMEOUT_MS) {
            AtomicReference<Reservation> ref = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            reservationRepository.findById(reservationId, (reservation, error) -> {
                ref.set(reservation);
                latch.countDown();
            });

            assertTrue("findById(reservation) timed out", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            latest = ref.get();
            if (latest != null) return latest;

            Thread.sleep(EVENTUAL_POLL_MS);
        }

        fail("Timed out waiting for reservation: " + reservationId);
        return latest;
    }
}