package com.example.ticketnoob.service;

import com.example.ticketnoob.model.Event;
import com.example.ticketnoob.model.Reservation;
import com.example.ticketnoob.model.Status;
import com.example.ticketnoob.repository.EventRepository;
import com.example.ticketnoob.repository.ReservationRepository;

import java.time.LocalDateTime;
import java.util.List;

public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              EventRepository eventRepository) {
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
    }

    public void createReservation(String userId,
                                  String eventId,
                                  ServiceCallback<Reservation> callback) {

        final String trimmedUserId = safeTrim(userId);
        final String trimmedEventId = safeTrim(eventId);

        if (trimmedUserId.isEmpty()) {
            callback.onComplete(ServiceResult.error("User ID required", "userId"));
            return;
        }

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

            if (!event.isActive()) {
                callback.onComplete(ServiceResult.error(
                        "Cannot reserve a cancelled event",
                        "status"
                ));
                return;
            }

            if (!event.hasAvailableSeats()) {
                callback.onComplete(ServiceResult.error(
                        "No seats available",
                        "availableSeats"
                ));
                return;
            }

            reservationRepository.findActiveByUserIdAndEventId(
                    trimmedUserId,
                    trimmedEventId,
                    (existingReservations, existingError) -> {

                        if (existingReservations == null) {
                            callback.onComplete(ServiceResult.error(
                                    existingError != null ? existingError : "Failed to check existing reservations",
                                    "repository"
                            ));
                            return;
                        }

                        if (!existingReservations.isEmpty()) {
                            callback.onComplete(ServiceResult.error(
                                    "User already has an active reservation for this event",
                                    "reservation"
                            ));
                            return;
                        }

                        event.setAvailableSeats(event.getAvailableSeats() - 1);

                        eventRepository.update(event, (eventUpdated, updateError) -> {
                            if (eventUpdated == null || !eventUpdated) {
                                callback.onComplete(ServiceResult.error(
                                        updateError != null ? updateError : "Failed to update seat count",
                                        "repository"
                                ));
                                return;
                            }

                            Reservation reservation = new Reservation(
                                    trimmedUserId,
                                    trimmedEventId,
                                    LocalDateTime.now().toString()
                            );
                            reservation.setStatusEnum(Status.ACTIVE);

                            reservationRepository.save(reservation, (saved, saveError) -> {
                                if (saved == null || !saved) {
                                    // rollback seat count if reservation save fails
                                    event.setAvailableSeats(event.getAvailableSeats() + 1);
                                    eventRepository.update(event, (rollbackSuccess, rollbackError) ->
                                            callback.onComplete(ServiceResult.error(
                                                    saveError != null ? saveError : "Failed to create reservation",
                                                    "repository"
                                            ))
                                    );
                                    return;
                                }

                                callback.onComplete(ServiceResult.success(reservation));
                            });
                        });
                    }
            );
        });
    }

    public void cancelReservation(String reservationId,
                                  ServiceCallback<Reservation> callback) {

        final String trimmedReservationId = safeTrim(reservationId);

        if (trimmedReservationId.isEmpty()) {
            callback.onComplete(ServiceResult.error("Reservation ID required", "reservationId"));
            return;
        }

        reservationRepository.findById(trimmedReservationId, (reservation, reservationError) -> {
            if (reservation == null) {
                callback.onComplete(ServiceResult.error(
                        reservationError != null ? reservationError : "Reservation not found",
                        "reservationId"
                ));
                return;
            }

            if (reservation.isCancelled()) {
                callback.onComplete(ServiceResult.error(
                        "Reservation already cancelled",
                        "status"
                ));
                return;
            }

            eventRepository.findById(reservation.getEventId(), (event, eventError) -> {
                if (event == null) {
                    callback.onComplete(ServiceResult.error(
                            eventError != null ? eventError : "Associated event not found",
                            "eventId"
                    ));
                    return;
                }

                reservation.setStatusEnum(Status.CANCELLED);

                reservationRepository.update(reservation, (reservationUpdated, updateError) -> {
                    if (reservationUpdated == null || !reservationUpdated) {
                        callback.onComplete(ServiceResult.error(
                                updateError != null ? updateError : "Failed to cancel reservation",
                                "repository"
                        ));
                        return;
                    }

                    // Only restore a seat if the event is still active
                    if (event.isActive()) {
                        event.setAvailableSeats(event.getAvailableSeats() + 1);

                        eventRepository.update(event, (eventUpdated, eventUpdateError) -> {
                            if (eventUpdated == null || !eventUpdated) {
                                callback.onComplete(ServiceResult.error(
                                        eventUpdateError != null ? eventUpdateError : "Reservation cancelled, but failed to restore seat",
                                        "repository"
                                ));
                                return;
                            }

                            callback.onComplete(ServiceResult.success(reservation));
                        });
                    } else {
                        callback.onComplete(ServiceResult.success(reservation));
                    }
                });
            });
        });
    }

    public void getActiveReservationsForUser(String userId,
                                             ServiceCallback<List<Reservation>> callback) {

        final String trimmedUserId = safeTrim(userId);

        if (trimmedUserId.isEmpty()) {
            callback.onComplete(ServiceResult.error("User ID required", "userId"));
            return;
        }

        reservationRepository.findActiveByUserId(trimmedUserId, (reservations, error) -> {
            if (reservations == null) {
                callback.onComplete(ServiceResult.error(
                        error != null ? error : "Failed to load reservations",
                        "repository"
                ));
                return;
            }

            callback.onComplete(ServiceResult.success(reservations));
        });
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}