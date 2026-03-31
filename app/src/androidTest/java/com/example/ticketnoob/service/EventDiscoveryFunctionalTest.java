package com.example.ticketnoob.service;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.ticketnoob.model.Event;
import com.example.ticketnoob.model.Status;
import com.example.ticketnoob.repository.EventRepository;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class EventDiscoveryFunctionalTest {

	private static final long TIMEOUT_SECONDS = 25L;
	private static final long EVENTUAL_TIMEOUT_MS = 12000L;
	private static final long EVENTUAL_POLL_MS = 300L;

	private EventRepository eventRepository;
	private EventService eventService;
	private FirebaseFirestore firestore;
	private final List<String> createdEventIds = new ArrayList<>();
	private String runTag;

	@Before
	public void setUp() {
		eventRepository = new EventRepository();
		eventService = new EventService(eventRepository);
		firestore = FirebaseFirestore.getInstance();
		runTag = "func-" + System.currentTimeMillis();
	}

	@After
	public void tearDown() throws InterruptedException {
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
	public void viewListOfAllAvailableEvents_returnsOnlyActiveEventsWithSeats() throws InterruptedException {
		Event shouldAppear = createAndSaveEvent(
				"Visible Event " + runTag,
				"Available to users",
				"2026-09-10 19:00",
				"Montreal",
				"Music",
				120,
				45,
				Status.ACTIVE
		);

		Event noSeats = createAndSaveEvent(
				"No Seats Event " + runTag,
				"Fully booked",
				"2026-09-11 20:00",
				"Montreal",
				"Music",
				100,
				0,
				Status.ACTIVE
		);

		Event cancelled = createAndSaveEvent(
				"Cancelled Event " + runTag,
				"Should not be listed",
				"2026-09-12 20:00",
				"Montreal",
				"Music",
				100,
				10,
				Status.CANCELLED
		);

		List<Event> availableEvents = getAvailableEvents(shouldAppear.getId());
		List<String> ids = extractIds(availableEvents);

		assertTrue("Expected available event to be listed", ids.contains(shouldAppear.getId()));
		assertFalse("Expected fully-booked event to be hidden", ids.contains(noSeats.getId()));
		assertFalse("Expected cancelled event to be hidden", ids.contains(cancelled.getId()));
	}

	@Test
	public void searchEventsByKeyword_matchesTitleDescriptionLocationCategory() throws InterruptedException {
		Event titleEvent = createAndSaveEvent(
				"Tech Summit " + runTag,
				"General meetup",
				"2026-10-01 10:00",
				"Quebec City",
				"Conference",
				120,
				80,
				Status.ACTIVE
		);

		Event descriptionEvent = createAndSaveEvent(
				"Community Meetup " + runTag,
				"Includes workshop",
				"2026-10-02 10:00",
				"Montreal",
				"Workshop",
				120,
				80,
				Status.ACTIVE
		);

		Event locationAndCategoryEvent = createAndSaveEvent(
				"City Run " + runTag,
				"Outdoor run",
				"2026-10-03 08:00",
				"Laval",
				"Sports",
				200,
				120,
				Status.ACTIVE
		);

		List<Event> availableEvents = getAvailableEvents(titleEvent.getId());

		List<Event> titleMatches = eventService.applyKeywordSearch(availableEvents, "tech");
		assertTrue(extractIds(titleMatches).contains(titleEvent.getId()));

		List<Event> descriptionMatches = eventService.applyKeywordSearch(availableEvents, "workshop");
		assertTrue(extractIds(descriptionMatches).contains(descriptionEvent.getId()));

		List<Event> locationMatches = eventService.applyKeywordSearch(availableEvents, "laval");
		assertTrue(extractIds(locationMatches).contains(locationAndCategoryEvent.getId()));

		List<Event> categoryMatches = eventService.applyKeywordSearch(availableEvents, "sports");
		assertTrue(extractIds(categoryMatches).contains(locationAndCategoryEvent.getId()));
	}

	@Test
	public void filterByDateLocationCategory_returnsExpectedSubset() throws InterruptedException {
		Event expected = createAndSaveEvent(
				"Filter Match " + runTag,
				"Target event",
				"2026-11-15 14:00",
				"Montreal Downtown",
				"Music",
				150,
				90,
				Status.ACTIVE
		);

		createAndSaveEvent(
				"Wrong Date " + runTag,
				"Different date",
				"2026-11-16 14:00",
				"Montreal Downtown",
				"Music",
				150,
				90,
				Status.ACTIVE
		);

		createAndSaveEvent(
				"Wrong Location " + runTag,
				"Different location",
				"2026-11-15 14:00",
				"Toronto",
				"Music",
				150,
				90,
				Status.ACTIVE
		);

		createAndSaveEvent(
				"Wrong Category " + runTag,
				"Different category",
				"2026-11-15 14:00",
				"Montreal Downtown",
				"Theatre",
				150,
				90,
				Status.ACTIVE
		);

		List<Event> filtered = filterEvents("2026-11-15", "montreal", "music", true, expected.getId());
		List<String> ids = extractIds(filtered);

		assertTrue("Expected target event to match all filters", ids.contains(expected.getId()));
		assertEquals("Expected exactly one event for combined filters", 1, countTestEvents(filtered));
	}

	@Test
	public void eventDetailView_returnsEventById() throws InterruptedException {
		Event created = createAndSaveEvent(
				"Detail Event " + runTag,
				"Detail description",
				"2026-12-01 19:30",
				"Ottawa",
				"Conference",
				250,
				125,
				Status.ACTIVE
		);

		ServiceResult<Event> result = awaitGetEventById(created.getId());

		assertNotNull("Expected non-null service result", result);
		assertTrue("Expected successful event lookup", result.success);
		assertNotNull("Expected event data", result.data);
		assertEquals(created.getId(), result.data.getId());
		assertEquals(created.getTitle(), result.data.getTitle());
		assertEquals(created.getLocation(), result.data.getLocation());
		assertEquals(created.getCategory(), result.data.getCategory());
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

	private List<Event> getAvailableEvents(String expectedEventId) throws InterruptedException {
		long start = System.currentTimeMillis();
		List<Event> latest = new ArrayList<>();

		while (System.currentTimeMillis() - start < EVENTUAL_TIMEOUT_MS) {
			latest = awaitGetAvailableEvents();
			if (extractIds(latest).contains(expectedEventId)) {
				return latest;
			}
			Thread.sleep(EVENTUAL_POLL_MS);
		}

		fail("Timed out waiting for event to be visible in available events: " + expectedEventId);
		return latest;
	}

	private List<Event> filterEvents(
			String date,
			String location,
			String category,
			boolean availableOnly,
			String expectedEventId
	) throws InterruptedException {
		long start = System.currentTimeMillis();
		List<Event> latest = new ArrayList<>();

		while (System.currentTimeMillis() - start < EVENTUAL_TIMEOUT_MS) {
			latest = awaitFilterEvents(date, location, category, availableOnly);
			if (extractIds(latest).contains(expectedEventId)) {
				return latest;
			}
			Thread.sleep(EVENTUAL_POLL_MS);
		}

		fail("Timed out waiting for event to match filters: " + expectedEventId);
		return latest;
	}

	private List<Event> awaitGetAvailableEvents() throws InterruptedException {
		AtomicReference<ServiceResult<List<Event>>> resultRef = new AtomicReference<>();
		CountDownLatch latch = new CountDownLatch(1);

		eventService.getAvailableEvents(result -> {
			resultRef.set(result);
			latch.countDown();
		});

		assertTrue("getAvailableEvents timed out", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

		ServiceResult<List<Event>> result = resultRef.get();
		assertNotNull("Expected non-null result for getAvailableEvents", result);
		assertTrue("Expected getAvailableEvents to succeed, got: " + result.message, result.success);

		return result.data == null ? new ArrayList<>() : result.data;
	}

	private List<Event> awaitFilterEvents(
			String date,
			String location,
			String category,
			boolean availableOnly
	) throws InterruptedException {
		AtomicReference<ServiceResult<List<Event>>> resultRef = new AtomicReference<>();
		CountDownLatch latch = new CountDownLatch(1);

		eventService.filterEvents(date, location, category, availableOnly, result -> {
			resultRef.set(result);
			latch.countDown();
		});

		assertTrue("filterEvents timed out", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

		ServiceResult<List<Event>> result = resultRef.get();
		assertNotNull("Expected non-null result for filterEvents", result);
		assertTrue("Expected filterEvents to succeed, got: " + result.message, result.success);

		return result.data == null ? new ArrayList<>() : result.data;
	}

	private ServiceResult<Event> awaitGetEventById(String eventId) throws InterruptedException {
		AtomicReference<ServiceResult<Event>> resultRef = new AtomicReference<>();
		CountDownLatch latch = new CountDownLatch(1);

		eventService.getEventById(eventId, result -> {
			resultRef.set(result);
			latch.countDown();
		});

		assertTrue("getEventById timed out", latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
		return resultRef.get();
	}

	private List<String> extractIds(List<Event> events) {
		List<String> ids = new ArrayList<>();
		if (events == null) {
			return ids;
		}

		for (Event event : events) {
			if (event != null && event.getId() != null) {
				ids.add(event.getId());
			}
		}
		return ids;
	}

	private int countTestEvents(List<Event> events) {
		int count = 0;
		if (events == null) {
			return count;
		}

		for (Event event : events) {
			if (event == null) {
				continue;
			}

			String title = event.getTitle();
			String id = event.getId();
			if ((title != null && title.contains(runTag)) || (id != null && id.contains(runTag))) {
				count++;
			}
		}

		return count;
	}
}
