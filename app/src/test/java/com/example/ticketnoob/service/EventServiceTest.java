package com.example.ticketnoob.service;

import com.example.ticketnoob.model.Event;
import com.example.ticketnoob.repository.EventRepository;
import com.example.ticketnoob.repository.RepositoryCallback;

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

class EventServiceTest {

    private EventService eventService;
    private EventRepository mockRepo;

    // Reusable test events
    private Event activeEventWithSeats;
    private Event activeEventNoSeats;
    private Event musicEventLondon;
    private Event sportsEventParis;
    private Event musicEventParis;

    @BeforeEach
    void setUp() {
        mockRepo = mock(EventRepository.class);
        eventService = new EventService(mockRepo);

        // Active event with seats available
        activeEventWithSeats = new Event(
                "a1", "Concert A", "Desc", "2026-06-15",
                "London", "Music", 100, 50, 25.0, "ACTIVE", "admin1"
        );

        // Active event but fully booked
        activeEventNoSeats = new Event(
                "a2", "Concert B", "Desc", "2026-06-20",
                "Paris", "Music", 100, 0, 30.0, "ACTIVE", "admin1"
        );

        // Music event in London
        musicEventLondon = new Event(
                "b1", "Jazz Night", "Desc", "2026-07-01",
                "London", "Music", 200, 100, 15.0, "ACTIVE", "admin1"
        );

        // Sports event in Paris
        sportsEventParis = new Event(
                "b2", "Football Match", "Desc", "2026-07-01",
                "Paris", "Sports", 500, 200, 40.0, "ACTIVE", "admin1"
        );

        // Music event in Paris
        musicEventParis = new Event(
                "b3", "Paris Concert", "Desc", "2026-08-10",
                "Paris", "Music", 300, 150, 50.0, "ACTIVE", "admin1"
        );
    }

    // =========================================================================
    // getAvailableEvents
    // =========================================================================

    @Test
    void getAvailableEvents_returnsOnlyEventsWithSeats() {
        @SuppressWarnings("unchecked")
        ServiceCallback<List<Event>> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<List<Event>>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<List<Event>> repoCallback = invocation.getArgument(0);
            repoCallback.onComplete(Arrays.asList(activeEventWithSeats, activeEventNoSeats), null);
            return null;
        }).when(mockRepo).getAllActiveEvents(any());

        eventService.getAvailableEvents(callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<List<Event>> result = captor.getValue();

        assertTrue(result.success);
        assertEquals(1, result.data.size());
        assertEquals("a1", result.data.get(0).getId());
    }

    @Test
    void getAvailableEvents_emptyList_returnsEmptySuccess() {
        @SuppressWarnings("unchecked")
        ServiceCallback<List<Event>> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<List<Event>>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<List<Event>> repoCallback = invocation.getArgument(0);
            repoCallback.onComplete(Collections.emptyList(), null);
            return null;
        }).when(mockRepo).getAllActiveEvents(any());

        eventService.getAvailableEvents(callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<List<Event>> result = captor.getValue();

        assertTrue(result.success);
        assertTrue(result.data.isEmpty());
    }

    @Test
    void getAvailableEvents_repositoryFails_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<List<Event>> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<List<Event>>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<List<Event>> repoCallback = invocation.getArgument(0);
            repoCallback.onComplete(null, "Firestore unavailable");
            return null;
        }).when(mockRepo).getAllActiveEvents(any());

        eventService.getAvailableEvents(callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<List<Event>> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Firestore unavailable", result.message);
        assertEquals("repository", result.field);
    }

    // =========================================================================
    // getEventById
    // =========================================================================

    @Test
    void getEventById_nullId_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Event> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Event>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        eventService.getEventById(null, callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Event> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("eventId", result.field);
    }

    @Test
    void getEventById_notFound_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Event> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Event>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Event> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(null, "Event not found");
            return null;
        }).when(mockRepo).findById(eq("missing-id"), any());

        eventService.getEventById("missing-id", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Event> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Event not found", result.message);
    }

    @Test
    void getEventById_found_returnsEvent() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Event> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Event>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Event> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(activeEventWithSeats, null);
            return null;
        }).when(mockRepo).findById(eq("a1"), any());

        eventService.getEventById("a1", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<Event> result = captor.getValue();

        assertTrue(result.success);
        assertEquals("a1", result.data.getId());
        assertEquals("Concert A", result.data.getTitle());
    }

    // =========================================================================
    // filterEvents — in-memory filter logic
    // =========================================================================

    @Test
    void filterEvents_byCategory_returnsMatchingEvents() {
        @SuppressWarnings("unchecked")
        ServiceCallback<List<Event>> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<List<Event>>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        List<Event> allEvents = Arrays.asList(musicEventLondon, sportsEventParis, musicEventParis);

        doAnswer(invocation -> {
            RepositoryCallback<List<Event>> repoCallback = invocation.getArgument(0);
            repoCallback.onComplete(allEvents, null);
            return null;
        }).when(mockRepo).getAllActiveEvents(any());

        eventService.filterEvents(null, null, "Music", false, callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<List<Event>> result = captor.getValue();

        assertTrue(result.success);
        assertEquals(2, result.data.size());
        assertTrue(result.data.stream().allMatch(e -> e.getCategory().equals("Music")));
    }

    @Test
    void filterEvents_byLocation_returnsMatchingEvents() {
        @SuppressWarnings("unchecked")
        ServiceCallback<List<Event>> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<List<Event>>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        List<Event> allEvents = Arrays.asList(musicEventLondon, sportsEventParis, musicEventParis);

        doAnswer(invocation -> {
            RepositoryCallback<List<Event>> repoCallback = invocation.getArgument(0);
            repoCallback.onComplete(allEvents, null);
            return null;
        }).when(mockRepo).getAllActiveEvents(any());

        eventService.filterEvents(null, "Paris", null, false, callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<List<Event>> result = captor.getValue();

        assertTrue(result.success);
        assertEquals(2, result.data.size());
        assertTrue(result.data.stream().allMatch(e -> e.getLocation().equals("Paris")));
    }

    @Test
    void filterEvents_byDate_returnsMatchingEvents() {
        @SuppressWarnings("unchecked")
        ServiceCallback<List<Event>> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<List<Event>>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        List<Event> allEvents = Arrays.asList(musicEventLondon, sportsEventParis, musicEventParis);

        doAnswer(invocation -> {
            RepositoryCallback<List<Event>> repoCallback = invocation.getArgument(0);
            repoCallback.onComplete(allEvents, null);
            return null;
        }).when(mockRepo).getAllActiveEvents(any());

        // musicEventLondon and sportsEventParis both have date "2026-07-01"
        eventService.filterEvents("2026-07-01", null, null, false, callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<List<Event>> result = captor.getValue();

        assertTrue(result.success);
        assertEquals(2, result.data.size());
        assertTrue(result.data.stream().allMatch(e -> e.getDate().equals("2026-07-01")));
    }

    @Test
    void filterEvents_byCategoryAndLocation_returnsMatchingEvents() {
        @SuppressWarnings("unchecked")
        ServiceCallback<List<Event>> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<List<Event>>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        List<Event> allEvents = Arrays.asList(musicEventLondon, sportsEventParis, musicEventParis);

        doAnswer(invocation -> {
            RepositoryCallback<List<Event>> repoCallback = invocation.getArgument(0);
            repoCallback.onComplete(allEvents, null);
            return null;
        }).when(mockRepo).getAllActiveEvents(any());

        // Only musicEventParis matches Music + Paris
        eventService.filterEvents(null, "Paris", "Music", false, callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<List<Event>> result = captor.getValue();

        assertTrue(result.success);
        assertEquals(1, result.data.size());
        assertEquals("b3", result.data.get(0).getId());
    }

    @Test
    void filterEvents_availableOnly_excludesFullyBookedEvents() {
        @SuppressWarnings("unchecked")
        ServiceCallback<List<Event>> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<List<Event>>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        // activeEventNoSeats has 0 available seats
        List<Event> allEvents = Arrays.asList(activeEventWithSeats, activeEventNoSeats);

        doAnswer(invocation -> {
            RepositoryCallback<List<Event>> repoCallback = invocation.getArgument(0);
            repoCallback.onComplete(allEvents, null);
            return null;
        }).when(mockRepo).getAllActiveEvents(any());

        eventService.filterEvents(null, null, null, true, callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<List<Event>> result = captor.getValue();

        assertTrue(result.success);
        assertEquals(1, result.data.size());
        assertEquals("a1", result.data.get(0).getId());
    }

    @Test
    void filterEvents_noFilters_returnsAllActiveEvents() {
        @SuppressWarnings("unchecked")
        ServiceCallback<List<Event>> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<List<Event>>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        List<Event> allEvents = Arrays.asList(musicEventLondon, sportsEventParis, musicEventParis);

        doAnswer(invocation -> {
            RepositoryCallback<List<Event>> repoCallback = invocation.getArgument(0);
            repoCallback.onComplete(allEvents, null);
            return null;
        }).when(mockRepo).getAllActiveEvents(any());

        eventService.filterEvents(null, null, null, false, callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<List<Event>> result = captor.getValue();

        assertTrue(result.success);
        assertEquals(3, result.data.size());
    }

    @Test
    void filterEvents_noMatchingResults_returnsEmptyList() {
        @SuppressWarnings("unchecked")
        ServiceCallback<List<Event>> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<List<Event>>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        List<Event> allEvents = Arrays.asList(musicEventLondon, sportsEventParis);

        doAnswer(invocation -> {
            RepositoryCallback<List<Event>> repoCallback = invocation.getArgument(0);
            repoCallback.onComplete(allEvents, null);
            return null;
        }).when(mockRepo).getAllActiveEvents(any());

        eventService.filterEvents(null, null, "Theatre", false, callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<List<Event>> result = captor.getValue();

        assertTrue(result.success);
        assertTrue(result.data.isEmpty());
    }

    // =========================================================================
    // Direct unit tests on in-memory helpers (no mock needed)
    // =========================================================================

    @Test
    void filterByAvailability_excludesEventsWithNoSeats() {
        List<Event> events = Arrays.asList(activeEventWithSeats, activeEventNoSeats);
        List<Event> result = eventService.filterByAvailability(events);

        assertEquals(1, result.size());
        assertEquals("a1", result.get(0).getId());
    }

    @Test
    void filterByAvailability_nullList_returnsEmptyList() {
        List<Event> result = eventService.filterByAvailability(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void applyFilters_caseInsensitiveCategory() {
        List<Event> events = Arrays.asList(musicEventLondon, sportsEventParis);
        List<Event> result = eventService.applyFilters(events, null, null, "music", false);

        assertEquals(1, result.size());
        assertEquals("b1", result.get(0).getId());
    }

    @Test
    void applyFilters_locationPartialMatch() {
        List<Event> events = Arrays.asList(musicEventLondon, sportsEventParis, musicEventParis);
        // "par" should match "Paris"
        List<Event> result = eventService.applyFilters(events, null, "par", null, false);

        assertEquals(2, result.size());
    }
}