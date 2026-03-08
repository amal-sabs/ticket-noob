package com.example.ticketnoob.service;

import com.example.ticketnoob.model.Event;
import com.example.ticketnoob.model.Reservation;
import com.example.ticketnoob.model.Status;
import com.example.ticketnoob.model.User;
import com.example.ticketnoob.repository.EventRepository;
import com.example.ticketnoob.repository.ReservationRepository;

import java.util.List;

public class AdminEventService {

    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;

    public AdminEventService(EventRepository eventRepository,
                             ReservationRepository reservationRepository) {
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
    }

    public void createEvent(User user,
                            String title,
                            String description,
                            String date,
                            String location,
                            String category,
                            int capacity,
                            double price,
                            ServiceCallback<Event> callback) {

        if (!isAdmin(user, callback)) return;

        final String trimmedTitle = safeTrim(title);
        final String trimmedDescription = safeTrim(description);
        final String trimmedDate = safeTrim(date);
        final String trimmedLocation = safeTrim(location);
        final String trimmedCategory = safeTrim(category);

        ServiceResult<Event> validation = validateEventFields(
                trimmedTitle,
                trimmedDescription,
                trimmedDate,
                trimmedLocation,
                trimmedCategory,
                capacity,
                price
        );

        if (!validation.success) {
            callback.onComplete(validation);
            return;
        }

        Event event = new Event(
                trimmedTitle,
                trimmedDescription,
                trimmedDate,
                trimmedLocation,
                trimmedCategory,
                capacity,
                capacity,
                price,
                Status.ACTIVE
        );

        eventRepository.save(event, (success, error) -> {
            if (success == null || !success) {
                callback.onComplete(ServiceResult.error(
                        error != null ? error : "Failed to create event",
                        "repository"
                ));
                return;
            }

            callback.onComplete(ServiceResult.success(event));
        });
    }

    public void updateEvent(User user,
                            String eventId,
                            String title,
                            String description,
                            String date,
                            String location,
                            String category,
                            int capacity,
                            double price,
                            ServiceCallback<Event> callback) {

        if (!isAdmin(user, callback)) return;

        final String trimmedEventId = safeTrim(eventId);
        final String trimmedTitle = safeTrim(title);
        final String trimmedDescription = safeTrim(description);
        final String trimmedDate = safeTrim(date);
        final String trimmedLocation = safeTrim(location);
        final String trimmedCategory = safeTrim(category);

        if (trimmedEventId.isEmpty()) {
            callback.onComplete(ServiceResult.error("Event ID required", "eventId"));
            return;
        }

        ServiceResult<Event> validation = validateEventFields(
                trimmedTitle,
                trimmedDescription,
                trimmedDate,
                trimmedLocation,
                trimmedCategory,
                capacity,
                price
        );

        if (!validation.success) {
            callback.onComplete(validation);
            return;
        }

        eventRepository.findById(trimmedEventId, (existingEvent, findError) -> {
            if (existingEvent == null) {
                callback.onComplete(ServiceResult.error(
                        findError != null ? findError : "Event not found",
                        "eventId"
                ));
                return;
            }

            int bookedSeats = existingEvent.getCapacity() - existingEvent.getAvailableSeats();

            if (capacity < bookedSeats) {
                callback.onComplete(ServiceResult.error(
                        "Capacity cannot be less than booked seats",
                        "capacity"
                ));
                return;
            }

            existingEvent.setTitle(trimmedTitle);
            existingEvent.setDescription(trimmedDescription);
            existingEvent.setDate(trimmedDate);
            existingEvent.setLocation(trimmedLocation);
            existingEvent.setCategory(trimmedCategory);
            existingEvent.setCapacity(capacity);
            existingEvent.setAvailableSeats(capacity - bookedSeats);
            existingEvent.setPrice(price);

            eventRepository.update(existingEvent, (success, error) -> {
                if (success == null || !success) {
                    callback.onComplete(ServiceResult.error(
                            error != null ? error : "Failed to update event",
                            "repository"
                    ));
                    return;
                }

                callback.onComplete(ServiceResult.success(existingEvent));
            });
        });
    }

    public void cancelEvent(User user,
                            String eventId,
                            ServiceCallback<Event> callback) {

        if (!isAdmin(user, callback)) return;

        final String trimmedEventId = safeTrim(eventId);

        if (trimmedEventId.isEmpty()) {
            callback.onComplete(ServiceResult.error("Event ID required", "eventId"));
            return;
        }

        eventRepository.findById(trimmedEventId, (event, eventError) -> {
            if (event == null) {
                callback.onComplete(ServiceResult.error(
                        eventError != null ? eventError : "Event not found",
                        "eventId"
                ));
                return;
            }

            if (event.isCancelled()) {
                callback.onComplete(ServiceResult.error(
                        "Event is already cancelled",
                        "status"
                ));
                return;
            }

            event.setStatusEnum(Status.CANCELLED);

            eventRepository.update(event, (eventUpdated, updateError) -> {
                if (eventUpdated == null || !eventUpdated) {
                    callback.onComplete(ServiceResult.error(
                            updateError != null ? updateError : "Failed to cancel event",
                            "repository"
                    ));
                    return;
                }

                cancelAllReservationsForEvent(event, callback);
            });
        });
    }

    private void cancelAllReservationsForEvent(Event event,
                                               ServiceCallback<Event> callback) {
        reservationRepository.findActiveByEventId(event.getId(), (reservations, error) -> {
            if (reservations == null) {
                callback.onComplete(ServiceResult.error(
                        error != null ? error : "Event cancelled, but failed to fetch reservations",
                        "repository"
                ));
                return;
            }

            if (reservations.isEmpty()) {
                callback.onComplete(ServiceResult.success(event));
                return;
            }

            cancelReservationsRecursively(reservations, 0, event, callback);
        });
    }

    private void cancelReservationsRecursively(List<Reservation> reservations,
                                               int index,
                                               Event event,
                                               ServiceCallback<Event> callback) {
        if (index >= reservations.size()) {
            callback.onComplete(ServiceResult.success(event));
            return;
        }

        Reservation reservation = reservations.get(index);
        reservation.setStatusEnum(Status.CANCELLED);

        reservationRepository.update(reservation, (success, error) -> {
            if (success == null || !success) {
                callback.onComplete(ServiceResult.error(
                        error != null ? error : "Failed to cancel reservations for event",
                        "repository"
                ));
                return;
            }

            cancelReservationsRecursively(reservations, index + 1, event, callback);
        });
    }

    private <T> boolean isAdmin(User user, ServiceCallback<T> callback) {
        if (user == null) {
            callback.onComplete(ServiceResult.error("User required", "user"));
            return false;
        }

        if (!user.isAdmin()) {
            callback.onComplete(ServiceResult.error(
                    "Only admins can perform this action",
                    "role"
            ));
            return false;
        }

        return true;
    }

    private ServiceResult<Event> validateEventFields(String title,
                                                     String description,
                                                     String date,
                                                     String location,
                                                     String category,
                                                     int capacity,
                                                     double price) {

        if (title.isEmpty()) {
            return ServiceResult.error("Title required", "title");
        }

        if (description.isEmpty()) {
            return ServiceResult.error("Description required", "description");
        }

        if (date.isEmpty()) {
            return ServiceResult.error("Date required", "date");
        }

        if (location.isEmpty()) {
            return ServiceResult.error("Location required", "location");
        }

        if (category.isEmpty()) {
            return ServiceResult.error("Category required", "category");
        }

        if (capacity <= 0) {
            return ServiceResult.error("Capacity must be greater than 0", "capacity");
        }

        if (price < 0) {
            return ServiceResult.error("Price cannot be negative", "price");
        }

        return ServiceResult.success(null);
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}