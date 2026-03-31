package com.example.ticketnoob.service;

import com.example.ticketnoob.model.Event;
import com.example.ticketnoob.repository.EventRepository;

import java.util.ArrayList;
import java.util.List;

public class EventService {
    private final EventRepository eventRepository;

    public EventService(EventRepository repo){
        this.eventRepository = repo;
    }

    // Get all active events with no filters
    public void getAvailableEvents(ServiceCallback<List<Event>> callback){
        eventRepository.getAllActiveEvents((events, error) -> {
            if (events == null){
                callback.onComplete(ServiceResult.error(
                        error != null ? error : "Failed to load events",
                        "repository"
                ));
                return;
            }

            List<Event> available = filterByAvailability(events);
            callback.onComplete(ServiceResult.success(available));
        });
    }

    // Get single event by ID
    public void getEventById(String eventId, ServiceCallback<Event> callback){
        if (eventId == null || eventId.trim().isEmpty()){
            callback.onComplete(ServiceResult.error("Event ID required", "eventId"));
            return;
        }

        eventRepository.findById(eventId.trim(), (event, error) -> {
            if (event == null) {
                callback.onComplete(ServiceResult.error(
                        error != null ? error : "Error not found",
                        "eventId"
                ));
                return;
            }

            callback.onComplete(ServiceResult.success(event));
        });
    }

    // Search & Filter (fetch all active events, then filters in-memory
    // null/empty means "ignore that filter"
    public void filterEvents(String date, String location, String category, boolean availableOnly, ServiceCallback<List<Event>> callback){
        eventRepository.getAllActiveEvents((events, error) -> {
            if (events == null){
                callback.onComplete(ServiceResult.error(
                        error != null ? error : "Failed to load events",
                        "repository"
                ));
                return;
            }

            List<Event> filtered = applyFilters(events, date, location, category, availableOnly);
            callback.onComplete(ServiceResult.success(filtered));
        });
    }

    // in-memory filter helpers (package private so they can be unit tested directly

    List<Event> filterByAvailability(List<Event> events){
        List<Event> result = new ArrayList<>();
        if (events == null) return result;

        for (Event event : events) {
            if (event.hasAvailableSeats()){
                result.add(event);
            }
        }
        return result;
    }

    List<Event> applyFilters(List<Event> events, String date, String location, String category, boolean availableOnly){
        List<Event> result = new ArrayList<>();
        if (events == null) return result;

        String dateTrimmed = safeTrim(date);
        String locationTrimmed = safeTrim(location);
        String categoryTrimmed = safeTrim(category);

        for (Event event : events){

            // Filter by date
            if (!dateTrimmed.isEmpty()){
                if(event.getDate() == null || !event.getDate().startsWith(dateTrimmed)){
                    continue;
                }
            }

            // Filter by location
            if (!locationTrimmed.isEmpty()){
                if (event.getLocation() == null || !event.getLocation().toLowerCase().contains(locationTrimmed.toLowerCase())){
                    continue;
                }
            }

            // Filter by category
            if (!categoryTrimmed.isEmpty()) {
                if (event.getCategory() == null
                        || !event.getCategory().equalsIgnoreCase(categoryTrimmed)) {
                    continue;
                }
            }

            // Filter by seat availability
            if (availableOnly && !event.hasAvailableSeats()) {
                continue;
            }

            result.add(event);
        }
        return result;
    }

    private static String safeTrim(String s){
        return s == null ? "" : s.trim();
    }

    public List<Event> applyKeywordSearch(List<Event> events, String keyword) {
        List<Event> result = new ArrayList<>();
        if (events == null) return result;

        String key = safeTrim(keyword).toLowerCase();

        if (key.isEmpty()) {
            return new ArrayList<>(events);
        }

        for (Event event : events) {
            String title = event.getTitle();
            String description = event.getDescription();
            String category = event.getCategory();
            String location = event.getLocation();

            if ((title != null && title.toLowerCase().contains(key)) ||
                    (description != null && description.toLowerCase().contains(key)) ||
                    (category != null && category.toLowerCase().contains(key)) ||
                    (location != null && location.toLowerCase().contains(key))) {
                result.add(event);
            }
        }

        return result;
    }
}
