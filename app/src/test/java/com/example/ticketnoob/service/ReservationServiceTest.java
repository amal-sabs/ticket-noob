package com.example.ticketnoob.service;

import com.example.ticketnoob.model.Event;
import com.example.ticketnoob.model.Reservation;
import com.example.ticketnoob.model.Status;
import com.example.ticketnoob.repository.EventRepository;
import com.example.ticketnoob.repository.RepositoryCallback;
import com.example.ticketnoob.repository.ReservationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReservationServiceTest {

    private ReservationService reservationService;
    private ReservationRepository mockReservationRepo;
    private EventRepository mockEventRepo;

    private Event activeEventWithSeats;
    private Event activeEventNoSeats;
    private Event cancelledEvent;
    private Reservation activeReservation;
    private Reservation cancelledReservation;

    @BeforeEach
    void setUp() {
        mockReservationRepo = mock(ReservationRepository.class);
        mockEventRepo = mock(EventRepository.class);

        reservationService = new ReservationService(mockReservationRepo, mockEventRepo);

        activeEventWithSeats = new Event(
                "e1",
                "Concert",
                "Live concert",
                "2026-06-15",
                "Montreal",
                "Music",
                100,
                10,
                50.0,
                Status.ACTIVE
        );

        activeEventNoSeats = new Event(
                "e2",
                "Sold Out Show",
                "Desc",
                "2026-06-20",
                "Montreal",
                "Music",
                100,
                0,
                60.0,
                Status.ACTIVE
        );

        cancelledEvent = new Event(
                "e3",
                "Cancelled Show",
                "Desc",
                "2026-06-25",
                "Laval",
                "Music",
                100,
                20,
                40.0,
                Status.CANCELLED
        );

        activeReservation = new Reservation(
                "r1",
                "u1",
                "e1",
                Status.ACTIVE,
                "2026-03-07T10:00:00"
        );

        cancelledReservation = new Reservation(
                "r2",
                "u1",
                "e1",
                Status.CANCELLED,
                "2026-03-07T10:00:00"
        );
    }

    // =========================================================================
    // createReservation
    // =========================================================================

    @Test
    void createReservation_success() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Reservation> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Reservation>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Event> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(activeEventWithSeats, null);
            return null;
        }).when(mockEventRepo).findById(eq("e1"), any());

        doAnswer(invocation -> {
            RepositoryCallback<List<Reservation>> repoCallback = invocation.getArgument(2);
            repoCallback.onComplete(Collections.emptyList(), null);
            return null;
        }).when(mockReservationRepo).findActiveByUserIdAndEventId(eq("u1"), eq("e1"), any());

        doAnswer(invocation -> {
            RepositoryCallback<Boolean> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(true, null);
            return null;
        }).when(mockEventRepo).update(any(Event.class), any());

        doAnswer(invocation -> {
            RepositoryCallback<Boolean> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(true, null);
            return null;
        }).when(mockReservationRepo).save(any(Reservation.class), any());

        reservationService.createReservation("u1", "e1", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Reservation> result = captor.getValue();

        assertTrue(result.success);
        assertNotNull(result.data);
        assertEquals("u1", result.data.getUserId());
        assertEquals("e1", result.data.getEventId());
        assertEquals(Status.ACTIVE, result.data.getStatusEnum());
        assertEquals(9, activeEventWithSeats.getAvailableSeats());

        verify(mockEventRepo).update(any(Event.class), any());
        verify(mockReservationRepo).save(any(Reservation.class), any());
    }

    @Test
    void createReservation_emptyUserId_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Reservation> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Reservation>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        reservationService.createReservation("", "e1", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Reservation> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("User ID required", result.message);
        assertEquals("userId", result.field);

        verifyNoInteractions(mockEventRepo, mockReservationRepo);
    }

    @Test
    void createReservation_emptyEventId_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Reservation> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Reservation>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        reservationService.createReservation("u1", "", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Reservation> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Event ID required", result.message);
        assertEquals("eventId", result.field);

        verifyNoInteractions(mockEventRepo, mockReservationRepo);
    }

    @Test
    void createReservation_eventNotFound_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Reservation> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Reservation>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Event> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(null, "Event not found");
            return null;
        }).when(mockEventRepo).findById(eq("missing"), any());

        reservationService.createReservation("u1", "missing", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Reservation> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Event not found", result.message);
        assertEquals("eventId", result.field);
    }

    @Test
    void createReservation_cancelledEvent_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Reservation> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Reservation>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Event> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(cancelledEvent, null);
            return null;
        }).when(mockEventRepo).findById(eq("e3"), any());

        reservationService.createReservation("u1", "e3", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Reservation> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Cannot reserve a cancelled event", result.message);
        assertEquals("status", result.field);
    }

    @Test
    void createReservation_noSeatsAvailable_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Reservation> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Reservation>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Event> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(activeEventNoSeats, null);
            return null;
        }).when(mockEventRepo).findById(eq("e2"), any());

        reservationService.createReservation("u1", "e2", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Reservation> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("No seats available", result.message);
        assertEquals("availableSeats", result.field);
    }

    @Test
    void createReservation_duplicateActiveReservation_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Reservation> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Reservation>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Event> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(activeEventWithSeats, null);
            return null;
        }).when(mockEventRepo).findById(eq("e1"), any());

        doAnswer(invocation -> {
            RepositoryCallback<List<Reservation>> repoCallback = invocation.getArgument(2);
            repoCallback.onComplete(Collections.singletonList(activeReservation), null);
            return null;
        }).when(mockReservationRepo).findActiveByUserIdAndEventId(eq("u1"), eq("e1"), any());

        reservationService.createReservation("u1", "e1", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Reservation> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("User already has an active reservation for this event", result.message);
        assertEquals("reservation", result.field);
        assertEquals(10, activeEventWithSeats.getAvailableSeats());

        verify(mockEventRepo, never()).update(any(Event.class), any());
        verify(mockReservationRepo, never()).save(any(Reservation.class), any());
    }

    @Test
    void createReservation_eventUpdateFails_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Reservation> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Reservation>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Event> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(activeEventWithSeats, null);
            return null;
        }).when(mockEventRepo).findById(eq("e1"), any());

        doAnswer(invocation -> {
            RepositoryCallback<List<Reservation>> repoCallback = invocation.getArgument(2);
            repoCallback.onComplete(Collections.emptyList(), null);
            return null;
        }).when(mockReservationRepo).findActiveByUserIdAndEventId(eq("u1"), eq("e1"), any());

        doAnswer(invocation -> {
            RepositoryCallback<Boolean> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(false, "Failed to update seat count");
            return null;
        }).when(mockEventRepo).update(any(Event.class), any());

        reservationService.createReservation("u1", "e1", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Reservation> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Failed to update seat count", result.message);
        assertEquals("repository", result.field);

        verify(mockReservationRepo, never()).save(any(Reservation.class), any());
    }

    // =========================================================================
    // cancelReservation
    // =========================================================================

    @Test
    void cancelReservation_success_restoresSeat() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Reservation> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Reservation>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        activeEventWithSeats.setAvailableSeats(9);

        doAnswer(invocation -> {
            RepositoryCallback<Reservation> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(activeReservation, null);
            return null;
        }).when(mockReservationRepo).findById(eq("r1"), any());

        doAnswer(invocation -> {
            RepositoryCallback<Event> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(activeEventWithSeats, null);
            return null;
        }).when(mockEventRepo).findById(eq("e1"), any());

        doAnswer(invocation -> {
            RepositoryCallback<Boolean> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(true, null);
            return null;
        }).when(mockReservationRepo).update(any(Reservation.class), any());

        doAnswer(invocation -> {
            RepositoryCallback<Boolean> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(true, null);
            return null;
        }).when(mockEventRepo).update(any(Event.class), any());

        reservationService.cancelReservation("r1", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Reservation> result = captor.getValue();

        assertTrue(result.success);
        assertNotNull(result.data);
        assertEquals(Status.CANCELLED, result.data.getStatusEnum());
        assertEquals(10, activeEventWithSeats.getAvailableSeats());
    }

    @Test
    void cancelReservation_emptyId_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Reservation> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Reservation>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        reservationService.cancelReservation("", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Reservation> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Reservation ID required", result.message);
        assertEquals("reservationId", result.field);

        verifyNoInteractions(mockEventRepo, mockReservationRepo);
    }

    @Test
    void cancelReservation_notFound_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Reservation> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Reservation>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Reservation> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(null, "Reservation not found");
            return null;
        }).when(mockReservationRepo).findById(eq("missing"), any());

        reservationService.cancelReservation("missing", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Reservation> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Reservation not found", result.message);
        assertEquals("reservationId", result.field);
    }

    @Test
    void cancelReservation_alreadyCancelled_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Reservation> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Reservation>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Reservation> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(cancelledReservation, null);
            return null;
        }).when(mockReservationRepo).findById(eq("r2"), any());

        reservationService.cancelReservation("r2", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Reservation> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Reservation already cancelled", result.message);
        assertEquals("status", result.field);
    }

    @Test
    void getActiveReservationsForUser_success() {
        @SuppressWarnings("unchecked")
        ServiceCallback<List<Reservation>> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<List<Reservation>>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<List<Reservation>> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(Collections.singletonList(activeReservation), null);
            return null;
        }).when(mockReservationRepo).findActiveByUserId(eq("u1"), any());

        reservationService.getActiveReservationsForUser("u1", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<List<Reservation>> result = captor.getValue();

        assertTrue(result.success);
        assertEquals(1, result.data.size());
        assertEquals("r1", result.data.get(0).getId());
    }

    @Test
    void getActiveReservationsForUser_emptyUserId_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<List<Reservation>> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<List<Reservation>>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        reservationService.getActiveReservationsForUser("", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<List<Reservation>> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("User ID required", result.message);
        assertEquals("userId", result.field);
    }
}