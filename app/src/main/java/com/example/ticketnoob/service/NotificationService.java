package com.example.ticketnoob.service;

import com.example.ticketnoob.model.Event;
import com.example.ticketnoob.model.User;
import com.example.ticketnoob.repository.UserRepository;

public class NotificationService {

    private final UserRepository userRepository;
    private final EmailSender emailSender;
    private final SmsSender smsSender;

    public NotificationService(UserRepository userRepository,
                               EmailSender emailSender,
                               SmsSender smsSender) {
        this.userRepository = userRepository;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
    }

    public void notifyBookingConfirmed(String userId,
                                       Event event,
                                       ServiceCallback<Boolean> callback) {

        if (event == null) {
            callback.onComplete(ServiceResult.error("Event required", "event"));
            return;
        }

        fetchUserAndSend(userId, event,
                "Booking Confirmed",
                "Your reservation for \"" + event.getTitle() + "\" on " + event.getDate() + " is confirmed.",
                callback);
    }

    public void notifyBookingCancelled(String userId,
                                       Event event,
                                       ServiceCallback<Boolean> callback) {

        if (event == null) {
            callback.onComplete(ServiceResult.error("Event required", "event"));
            return;
        }

        fetchUserAndSend(userId, event,
                "Booking Cancelled",
                "Your reservation for \"" + event.getTitle() + "\" on " + event.getDate() + " has been cancelled.",
                callback);
    }

    public void notifyEventCancelled(String userId,
                                     Event event,
                                     ServiceCallback<Boolean> callback) {

        if (event == null) {
            callback.onComplete(ServiceResult.error("Event required", "event"));
            return;
        }

        fetchUserAndSend(userId, event,
                "Event Cancelled",
                "The event \"" + event.getTitle() + "\" on " + event.getDate() + " has been cancelled. Your reservation has been automatically cancelled.",
                callback);
    }

    private void fetchUserAndSend(String userId,
                                  Event event,
                                  String subject,
                                  String message,
                                  ServiceCallback<Boolean> callback) {

        String trimmedUserId = safeTrim(userId);

        if (trimmedUserId.isEmpty()) {
            callback.onComplete(ServiceResult.error("User ID required", "userId"));
            return;
        }

        userRepository.findById(trimmedUserId, (user, error) -> {
            if (user == null) {
                callback.onComplete(ServiceResult.error(
                        error != null ? error : "User not found",
                        "userId"
                ));
                return;
            }

            sendNotification(user, subject, message, callback);
        });
    }

    private void sendNotification(User user,
                                  String subject,
                                  String message,
                                  ServiceCallback<Boolean> callback) {

        boolean hasEmail = user.getEmail() != null && !user.getEmail().isEmpty();
        boolean hasPhone = user.getPhone() != null && !user.getPhone().isEmpty();

        if (!hasEmail && !hasPhone) {
            callback.onComplete(ServiceResult.error(
                    "User has no email or phone for notifications",
                    "contact"
            ));
            return;
        }

        // Prefer email, fall back to SMS
        if (hasEmail) {
            emailSender.send(user.getEmail(), subject, message, (success, error) -> {
                if (success != null && success) {
                    callback.onComplete(ServiceResult.success(true));
                } else {
                    if (hasPhone) {
                        smsSender.send(user.getPhone(), message, (smsSuccess, smsError) -> {
                            if (smsSuccess != null && smsSuccess) {
                                callback.onComplete(ServiceResult.success(true));
                            } else {
                                callback.onComplete(ServiceResult.error(
                                        smsError != null ? smsError : "Failed to send notification",
                                        "notification"
                                ));
                            }
                        });
                    } else {
                        callback.onComplete(ServiceResult.error(
                                error != null ? error : "Failed to send email",
                                "notification"
                        ));
                    }
                }
            });
        } else {
            smsSender.send(user.getPhone(), message, (success, error) -> {
                if (success != null && success) {
                    callback.onComplete(ServiceResult.success(true));
                } else {
                    callback.onComplete(ServiceResult.error(
                            error != null ? error : "Failed to send SMS",
                            "notification"
                    ));
                }
            });
        }
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}