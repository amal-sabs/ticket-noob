package com.example.ticketnoob.service;

import com.example.ticketnoob.model.Event;
import com.example.ticketnoob.model.Reservation;
import com.example.ticketnoob.model.Status;
import com.example.ticketnoob.model.User;
import com.example.ticketnoob.repository.EventRepository;
import com.example.ticketnoob.repository.RepositoryCallback;
import com.example.ticketnoob.repository.ReservationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AdminEventServiceTest {

    private AdminEventService adminEventService;
    private EventRepository mockEventRepo;
    private ReservationRepository mockReservationRepo;

    private User adminUser;
    private User customerUser;

    private Event activeEvent;
    private Event cancelledEvent;

    private Reservation activeReservation1;
    private Reservation activeReservation2;

    @BeforeEach
    void setUp() {
        mockEventRepo = mock(EventRepository.class);
        mockReservationRepo = mock(ReservationRepository.class);

        adminEventService = new AdminEventService(mockEventRepo, mockReservationRepo);

        adminUser = new User("u1", "Admin", "admin@test.com", "", "pass", "ADMIN");
        customerUser = new User("u2", "Customer", "customer@test.com", "", "pass", "CUSTOMER");

        activeEvent = new Event(
                "e1",
                "Concert A",
                "Music concert",
                "2026-06-15",
                "Montreal",
                "Music",
                100,
                70,
                50.0,
                Status.ACTIVE
        );

        cancelledEvent = new Event(
                "e2",
                "Concert B",
                "Cancelled concert",
                "2026-06-20",
                "Laval",
                "Music",
                120,
                120,
                40.0,
                Status.CANCELLED
        );

        activeReservation1 = new Reservation(
                "r1",
                "u10",
                "e1",
                Status.ACTIVE,
                "1111111111"
        );

        activeReservation2 = new Reservation(
                "r2",
                "u11",
                "e1",
                Status.ACTIVE,
                "2222222222"
        );
    }

    // =========================================================================
    // createEvent
    // =========================================================================

    @Test
    void createEvent_success() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Event> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Event>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Boolean> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(true, null);
            return null;
        }).when(mockEventRepo).save(any(Event.class), any());

        adminEventService.createEvent(
                adminUser,
                "Jazz Night",
                "Live jazz event",
                "2026-07-01",
                "Montreal",
                "Music",
                200,
                35.0,
                callback
        );

        verify(callback).onComplete(captor.capture());
        ServiceResult<Event> result = captor.getValue();

        assertTrue(result.success);
        assertNotNull(result.data);
        assertEquals("Jazz Night", result.data.getTitle());
        assertEquals("Live jazz event", result.data.getDescription());
        assertEquals("Montreal", result.data.getLocation());
        assertEquals("Music", result.data.getCategory());
        assertEquals(200, result.data.getCapacity());
        assertEquals(200, result.data.getAvailableSeats());
        assertEquals(Status.ACTIVE, result.data.getStatusEnum());

        verify(mockEventRepo).save(any(Event.class), any());
    }

    @Test
    void createEvent_nonAdmin_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Event> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Event>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        adminEventService.createEvent(
                customerUser,
                "Jazz Night",
                "Live jazz event",
                "2026-07-01",
                "Montreal",
                "Music",
                200,
                35.0,
                callback
        );

        verify(callback).onComplete(captor.capture());
        ServiceResult<Event> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Only admins can perform this action", result.message);
        assertEquals("role", result.field);

        verifyNoInteractions(mockEventRepo, mockReservationRepo);
    }

    @Test
    void createEvent_emptyTitle_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Event> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Event>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        adminEventService.createEvent(
                adminUser,
                "",
                "Live jazz event",
                "2026-07-01",
                "Montreal",
                "Music",
                200,
                35.0,
                callback
        );

        verify(callback).onComplete(captor.capture());
        ServiceResult<Event> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Title required", result.message);
        assertEquals("title", result.field);

        verifyNoInteractions(mockEventRepo, mockReservationRepo);
    }

    @Test
    void createEvent_invalidCapacity_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Event> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Event>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        adminEventService.createEvent(
                adminUser,
                "Jazz Night",
                "Live jazz event",
                "2026-07-01",
                "Montreal",
                "Music",
                0,
                35.0,
                callback
        );

        verify(callback).onComplete(captor.capture());
        ServiceResult<Event> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Capacity must be greater than 0", result.message);
        assertEquals("capacity", result.field);

        verifyNoInteractions(mockEventRepo, mockReservationRepo);
    }

    @Test
    void createEvent_repositoryFails_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Event> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Event>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Boolean> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(false, "Failed to create event");
            return null;
        }).when(mockEventRepo).save(any(Event.class), any());

        adminEventService.createEvent(
                adminUser,
                "Jazz Night",
                "Live jazz event",
                "2026-07-01",
                "Montreal",
                "Music",
                200,
                35.0,
                callback
        );

        verify(callback).onComplete(captor.capture());
        ServiceResult<Event> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Failed to create event", result.message);
        assertEquals("repository", result.field);
    }

    // =========================================================================
    // updateEvent
    // =========================================================================

    @Test
    void updateEvent_success() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Event> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Event>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Event> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(activeEvent, null);
            return null;
        }).when(mockEventRepo).findById(eq("e1"), any());

        doAnswer(invocation -> {
            RepositoryCallback<Boolean> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(true, null);
            return null;
        }).when(mockEventRepo).update(any(Event.class), any());

        adminEventService.updateEvent(
                adminUser,
                "e1",
                "Updated Concert",
                "Updated description",
                "2026-07-10",
                "Quebec City",
                "Festival",
                120,
                75.0,
                callback
        );

        verify(callback).onComplete(captor.capture());
        ServiceResult<Event> result = captor.getValue();

        assertTrue(result.success);
        assertNotNull(result.data);
        assertEquals("Updated Concert", result.data.getTitle());
        assertEquals("Updated description", result.data.getDescription());
        assertEquals("2026-07-10", result.data.getDate());
        assertEquals("Quebec City", result.data.getLocation());
        assertEquals("Festival", result.data.getCategory());
        assertEquals(120, result.data.getCapacity());
        assertEquals(90, result.data.getAvailableSeats()); // 30 already booked, so 120 - 30 = 90
        assertEquals(75.0, result.data.getPrice());

        verify(mockEventRepo).update(any(Event.class), any());
    }

    @Test
    void updateEvent_nonAdmin_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Event> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Event>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        adminEventService.updateEvent(
                customerUser,
                "e1",
                "Updated Concert",
                "Updated description",
                "2026-07-10",
                "Quebec City",
                "Festival",
                120,
                75.0,
                callback
        );

        verify(callback).onComplete(captor.capture());
        ServiceResult<Event> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Only admins can perform this action", result.message);
        assertEquals("role", result.field);

        verifyNoInteractions(mockEventRepo, mockReservationRepo);
    }

    @Test
    void updateEvent_emptyEventId_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Event> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Event>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        adminEventService.updateEvent(
                adminUser,
                "",
                "Updated Concert",
                "Updated description",
                "2026-07-10",
                "Quebec City",
                "Festival",
                120,
                75.0,
                callback
        );

        verify(callback).onComplete(captor.capture());
        ServiceResult<Event> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Event ID required", result.message);
        assertEquals("eventId", result.field);
    }

    @Test
    void updateEvent_notFound_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Event> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Event>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Event> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(null, "Event not found");
            return null;
        }).when(mockEventRepo).findById(eq("missing"), any());

        adminEventService.updateEvent(
                adminUser,
                "missing",
                "Updated Concert",
                "Updated description",
                "2026-07-10",
                "Quebec City",
                "Festival",
                120,
                75.0,
                callback
        );

        verify(callback).onComplete(captor.capture());
        ServiceResult<Event> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Event not found", result.message);
        assertEquals("eventId", result.field);
    }

    @Test
    void updateEvent_capacityLessThanBookedSeats_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Event> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Event>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Event> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(activeEvent, null);
            return null;
        }).when(mockEventRepo).findById(eq("e1"), any());

        adminEventService.updateEvent(
                adminUser,
                "e1",
                "Updated Concert",
                "Updated description",
                "2026-07-10",
                "Quebec City",
                "Festival",
                20,   // booked seats are 30
                75.0,
                callback
        );

        verify(callback).onComplete(captor.capture());
        ServiceResult<Event> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Capacity cannot be less than booked seats", result.message);
        assertEquals("capacity", result.field);

        verify(mockEventRepo, never()).update(any(Event.class), any());
    }

    @Test
    void updateEvent_repositoryFails_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Event> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Event>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Event> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(activeEvent, null);
            return null;
        }).when(mockEventRepo).findById(eq("e1"), any());

        doAnswer(invocation -> {
            RepositoryCallback<Boolean> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(false, "Failed to update event");
            return null;
        }).when(mockEventRepo).update(any(Event.class), any());

        adminEventService.updateEvent(
                adminUser,
                "e1",
                "Updated Concert",
                "Updated description",
                "2026-07-10",
                "Quebec City",
                "Festival",
                120,
                75.0,
                callback
        );

        verify(callback).onComplete(captor.capture());
        ServiceResult<Event> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Failed to update event", result.message);
        assertEquals("repository", result.field);
    }

    // =========================================================================
    // cancelEvent
    // =========================================================================

    @Test
    void cancelEvent_success_withNoReservations() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Event> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Event>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Event> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(activeEvent, null);
            return null;
        }).when(mockEventRepo).findById(eq("e1"), any());

        doAnswer(invocation -> {
            RepositoryCallback<Boolean> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(true, null);
            return null;
        }).when(mockEventRepo).update(any(Event.class), any());

        doAnswer(invocation -> {
            RepositoryCallback<List<Reservation>> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(Collections.emptyList(), null);
            return null;
        }).when(mockReservationRepo).findActiveByEventId(eq("e1"), any());

        adminEventService.cancelEvent(adminUser, "e1", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Event> result = captor.getValue();

        assertTrue(result.success);
        assertNotNull(result.data);
        assertEquals(Status.CANCELLED, result.data.getStatusEnum());
    }

    @Test
    void cancelEvent_success_withReservations() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Event> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Event>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        List<Reservation> reservations = Arrays.asList(activeReservation1, activeReservation2);

        doAnswer(invocation -> {
            RepositoryCallback<Event> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(activeEvent, null);
            return null;
        }).when(mockEventRepo).findById(eq("e1"), any());

        doAnswer(invocation -> {
            RepositoryCallback<Boolean> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(true, null);
            return null;
        }).when(mockEventRepo).update(any(Event.class), any());

        doAnswer(invocation -> {
            RepositoryCallback<List<Reservation>> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(reservations, null);
            return null;
        }).when(mockReservationRepo).findActiveByEventId(eq("e1"), any());

        doAnswer(invocation -> {
            RepositoryCallback<Boolean> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(true, null);
            return null;
        }).when(mockReservationRepo).update(any(Reservation.class), any());

        adminEventService.cancelEvent(adminUser, "e1", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Event> result = captor.getValue();

        assertTrue(result.success);
        assertEquals(Status.CANCELLED, result.data.getStatusEnum());
        assertEquals(Status.CANCELLED, activeReservation1.getStatusEnum());
        assertEquals(Status.CANCELLED, activeReservation2.getStatusEnum());

        verify(mockReservationRepo, times(2)).update(any(Reservation.class), any());
    }

    @Test
    void cancelEvent_nonAdmin_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Event> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Event>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        adminEventService.cancelEvent(customerUser, "e1", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Event> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Only admins can perform this action", result.message);
        assertEquals("role", result.field);

        verifyNoInteractions(mockEventRepo, mockReservationRepo);
    }

    @Test
    void cancelEvent_emptyEventId_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Event> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Event>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        adminEventService.cancelEvent(adminUser, "", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Event> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Event ID required", result.message);
        assertEquals("eventId", result.field);
    }

    @Test
    void cancelEvent_notFound_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Event> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Event>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Event> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(null, "Event not found");
            return null;
        }).when(mockEventRepo).findById(eq("missing"), any());

        adminEventService.cancelEvent(adminUser, "missing", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Event> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Event not found", result.message);
        assertEquals("eventId", result.field);
    }

    @Test
    void cancelEvent_alreadyCancelled_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Event> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Event>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Event> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(cancelledEvent, null);
            return null;
        }).when(mockEventRepo).findById(eq("e2"), any());

        adminEventService.cancelEvent(adminUser, "e2", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Event> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Event is already cancelled", result.message);
        assertEquals("status", result.field);
    }

    @Test
    void cancelEvent_eventUpdateFails_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Event> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Event>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Event> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(activeEvent, null);
            return null;
        }).when(mockEventRepo).findById(eq("e1"), any());

        doAnswer(invocation -> {
            RepositoryCallback<Boolean> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(false, "Failed to cancel event");
            return null;
        }).when(mockEventRepo).update(any(Event.class), any());

        adminEventService.cancelEvent(adminUser, "e1", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Event> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Failed to cancel event", result.message);
        assertEquals("repository", result.field);
    }

    @Test
    void cancelEvent_fetchReservationsFails_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Event> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Event>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Event> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(activeEvent, null);
            return null;
        }).when(mockEventRepo).findById(eq("e1"), any());

        doAnswer(invocation -> {
            RepositoryCallback<Boolean> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(true, null);
            return null;
        }).when(mockEventRepo).update(any(Event.class), any());

        doAnswer(invocation -> {
            RepositoryCallback<List<Reservation>> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(null, "Failed to fetch reservations");
            return null;
        }).when(mockReservationRepo).findActiveByEventId(eq("e1"), any());

        adminEventService.cancelEvent(adminUser, "e1", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Event> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Failed to fetch reservations", result.message);
        assertEquals("repository", result.field);
    }
}