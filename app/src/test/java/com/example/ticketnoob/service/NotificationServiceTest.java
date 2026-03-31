package com.example.ticketnoob.service;

import com.example.ticketnoob.model.Event;
import com.example.ticketnoob.model.Status;
import com.example.ticketnoob.model.User;
import com.example.ticketnoob.repository.RepositoryCallback;
import com.example.ticketnoob.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private NotificationService notificationService;
    private UserRepository mockUserRepo;
    private EmailSender mockEmailSender;
    private SmsSender mockSmsSender;

    private User userWithEmail;
    private User userWithPhone;
    private User userWithBoth;
    private User userWithNeither;
    private Event sampleEvent;

    @BeforeEach
    void setUp() {
        mockUserRepo = mock(UserRepository.class);
        mockEmailSender = mock(EmailSender.class);
        mockSmsSender = mock(SmsSender.class);

        notificationService = new NotificationService(mockUserRepo, mockEmailSender, mockSmsSender);

        userWithEmail = new User("u1", "Alice", "alice@test.com", "", "pass", "CUSTOMER");
        userWithPhone = new User("u2", "Bob", "", "1234567890", "pass", "CUSTOMER");
        userWithBoth = new User("u3", "Carol", "carol@test.com", "9876543210", "pass", "CUSTOMER");
        userWithNeither = new User("u4", "Dave", "", "", "pass", "CUSTOMER");

        sampleEvent = new Event(
                "e1", "Jazz Night", "Live jazz", "2026-07-01",
                "Montreal", "Music", 200, 150, 35.0, Status.ACTIVE
        );
    }

    @Test
    void notifyBookingConfirmed_sendsEmail() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Boolean> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Boolean>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        stubUserLookup("u1", userWithEmail);
        stubEmailSuccess();

        notificationService.notifyBookingConfirmed("u1", sampleEvent, callback);

        verify(mockEmailSender).send(
                eq("alice@test.com"),
                eq("Booking Confirmed"),
                eq("Your reservation for \"Jazz Night\" on 2026-07-01 is confirmed."),
                any()
        );
        verify(callback).onComplete(captor.capture());
        assertTrue(captor.getValue().success);
    }

    @Test
    void notifyBookingConfirmed_sendsSmsWhenNoEmail() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Boolean> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Boolean>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        stubUserLookup("u2", userWithPhone);
        stubSmsSuccess();

        notificationService.notifyBookingConfirmed("u2", sampleEvent, callback);

        verify(mockSmsSender).send(
                eq("1234567890"),
                eq("Your reservation for \"Jazz Night\" on 2026-07-01 is confirmed."),
                any()
        );
        verifyNoInteractions(mockEmailSender);
        verify(callback).onComplete(captor.capture());
        assertTrue(captor.getValue().success);
    }

    @Test
    void notifyBookingConfirmed_fallsBackToSmsOnEmailFailure() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Boolean> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Boolean>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        stubUserLookup("u3", userWithBoth);
        stubEmailFailure("Email server down");
        stubSmsSuccess();

        notificationService.notifyBookingConfirmed("u3", sampleEvent, callback);

        verify(mockEmailSender).send(eq("carol@test.com"), any(), any(), any());
        verify(mockSmsSender).send(eq("9876543210"), contains("Jazz Night"), any());
        verify(callback).onComplete(captor.capture());
        assertTrue(captor.getValue().success);
    }

    @Test
    void notifyBookingConfirmed_smsFails_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Boolean> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Boolean>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        stubUserLookup("u2", userWithPhone);
        stubSmsFailure("SMS failed");

        notificationService.notifyBookingConfirmed("u2", sampleEvent, callback);

        verify(callback).onComplete(captor.capture());
        assertFalse(captor.getValue().success);
        assertEquals("SMS failed", captor.getValue().message);
    }

    @Test
    void notifyBookingConfirmed_nullEvent_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Boolean> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Boolean>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        notificationService.notifyBookingConfirmed("u1", null, callback);

        verify(callback).onComplete(captor.capture());
        assertFalse(captor.getValue().success);
        assertEquals("Event required", captor.getValue().message);
    }

    @Test
    void notifyBookingConfirmed_emptyUserId_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Boolean> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Boolean>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        notificationService.notifyBookingConfirmed("", sampleEvent, callback);

        verify(callback).onComplete(captor.capture());
        assertFalse(captor.getValue().success);
        assertEquals("User ID required", captor.getValue().message);
    }

    @Test
    void notifyBookingConfirmed_userNotFound_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Boolean> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Boolean>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        stubUserNotFound("u99");

        notificationService.notifyBookingConfirmed("u99", sampleEvent, callback);

        verify(callback).onComplete(captor.capture());
        assertFalse(captor.getValue().success);
        assertEquals("User not found", captor.getValue().message);
    }
    @Test
    void notifyBookingConfirmed_nullUserId_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Boolean> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Boolean>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        notificationService.notifyBookingConfirmed(null, sampleEvent, callback);

        verify(callback).onComplete(captor.capture());
        assertFalse(captor.getValue().success);
        assertEquals("User ID required", captor.getValue().message);
    }

    @Test
    void notifyBookingConfirmed_noContactInfo_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Boolean> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Boolean>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        stubUserLookup("u4", userWithNeither);

        notificationService.notifyBookingConfirmed("u4", sampleEvent, callback);

        verify(callback).onComplete(captor.capture());
        assertFalse(captor.getValue().success);
        assertEquals("User has no email or phone for notifications", captor.getValue().message);
    }

    @Test
    void notifyBookingCancelled_sendsEmail() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Boolean> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Boolean>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        stubUserLookup("u1", userWithEmail);
        stubEmailSuccess();

        notificationService.notifyBookingCancelled("u1", sampleEvent, callback);

        verify(mockEmailSender).send(eq("alice@test.com"), eq("Booking Cancelled"), contains("has been cancelled"), any());
        verify(callback).onComplete(captor.capture());
        assertTrue(captor.getValue().success);
    }

    @Test
    void notifyBookingCancelled_fallsBackToSms() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Boolean> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Boolean>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        stubUserLookup("u3", userWithBoth);
        stubEmailFailure("Email failed");
        stubSmsSuccess();

        notificationService.notifyBookingCancelled("u3", sampleEvent, callback);

        verify(mockSmsSender).send(
                eq("9876543210"),
                eq("Your reservation for \"Jazz Night\" on 2026-07-01 has been cancelled."),
                any()
        );

        verify(callback).onComplete(captor.capture());
        assertTrue(captor.getValue().success);
    }

    @Test
    void notifyEventCancelled_sendsEmail() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Boolean> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Boolean>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        stubUserLookup("u1", userWithEmail);
        stubEmailSuccess();

        notificationService.notifyEventCancelled("u1", sampleEvent, callback);

        verify(mockEmailSender).send(eq("alice@test.com"), eq("Event Cancelled"), contains("automatically cancelled"), any());
        verify(callback).onComplete(captor.capture());
        assertTrue(captor.getValue().success);
    }

    @Test
    void notifyEventCancelled_fallsBackToSms() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Boolean> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Boolean>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        stubUserLookup("u3", userWithBoth);
        stubEmailFailure("Email failed");
        stubSmsSuccess();

        notificationService.notifyEventCancelled("u3", sampleEvent, callback);

        verify(mockSmsSender).send(
                eq("9876543210"),
                eq("The event \"Jazz Night\" on 2026-07-01 has been cancelled. Your reservation has been automatically cancelled."),
                any()
        );

        verify(callback).onComplete(captor.capture());
        assertTrue(captor.getValue().success);
    }

    @Test
    void notifyEventCancelled_allSendersFail_returnsError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Boolean> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Boolean>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        stubUserLookup("u3", userWithBoth);
        stubEmailFailure("Email failed");
        stubSmsFailure("SMS failed");

        notificationService.notifyEventCancelled("u3", sampleEvent, callback);

        verify(callback).onComplete(captor.capture());
        assertFalse(captor.getValue().success);
        assertEquals("SMS failed", captor.getValue().message);
    }

    @Test
    void notifyBookingConfirmed_emailFails_noPhone_returnsEmailError() {
        @SuppressWarnings("unchecked")
        ServiceCallback<Boolean> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<Boolean>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        stubUserLookup("u1", userWithEmail); // email only
        stubEmailFailure("Email failed");

        notificationService.notifyBookingConfirmed("u1", sampleEvent, callback);

        verify(callback).onComplete(captor.capture());
        assertFalse(captor.getValue().success);
        assertEquals("Email failed", captor.getValue().message);
    }

    // Helpers
    private void stubUserLookup(String userId, User user) {
        doAnswer(invocation -> {
            RepositoryCallback<User> cb = invocation.getArgument(1);
            cb.onComplete(user, null);
            return null;
        }).when(mockUserRepo).findById(eq(userId), any());
    }

    private void stubUserNotFound(String userId) {
        doAnswer(invocation -> {
            RepositoryCallback<User> cb = invocation.getArgument(1);
            cb.onComplete(null, "User not found");
            return null;
        }).when(mockUserRepo).findById(eq(userId), any());
    }

    private void stubEmailSuccess() {
        doAnswer(invocation -> {
            RepositoryCallback<Boolean> cb = invocation.getArgument(3);
            cb.onComplete(true, null);
            return null;
        }).when(mockEmailSender).send(any(), any(), any(), any());
    }

    private void stubEmailFailure(String error) {
        doAnswer(invocation -> {
            RepositoryCallback<Boolean> cb = invocation.getArgument(3);
            cb.onComplete(false, error);
            return null;
        }).when(mockEmailSender).send(any(), any(), any(), any());
    }

    private void stubSmsSuccess() {
        doAnswer(invocation -> {
            RepositoryCallback<Boolean> cb = invocation.getArgument(2);
            cb.onComplete(true, null);
            return null;
        }).when(mockSmsSender).send(any(), any(), any());
    }

    private void stubSmsFailure(String error) {
        doAnswer(invocation -> {
            RepositoryCallback<Boolean> cb = invocation.getArgument(2);
            cb.onComplete(false, error);
            return null;
        }).when(mockSmsSender).send(any(), any(), any());
    }
}