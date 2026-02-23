package com.example.ticketnoob.service;

import com.example.ticketnoob.model.User;
import com.example.ticketnoob.repository.RepositoryCallback;
import com.example.ticketnoob.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;

class RegistrationServiceTest {

    private RegistrationService registrationService;
    private UserRepository mockRepo;

    @BeforeEach
    void setUp() {
        mockRepo = mock(UserRepository.class);
        registrationService = new RegistrationService(mockRepo);
    }

    @Test
    void register_emptyEmailAndPhone() {

        @SuppressWarnings("unchecked")
        ServiceCallback<User> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<User>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        registrationService.register("John Doe", "", "", "password123", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<User> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Provide email OR phone", result.message);
        assertEquals("email_phone", result.field);
    }

    @Test
    void register_invalidEmailFormat() {

        @SuppressWarnings("unchecked")
        ServiceCallback<User> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<User>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        registrationService.register(
                "John Doe",
                "invalid-email",
                "",
                "password123",
                callback
        );

        verify(callback).onComplete(captor.capture());
        ServiceResult<User> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Invalid email format", result.message);
        assertEquals("email", result.field);
    }

    @Test
    void register_invalidPhoneFormat() {

        @SuppressWarnings("unchecked")
        ServiceCallback<User> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<User>> captor = ArgumentCaptor.forClass(ServiceResult.class);


        registrationService.register(
                "John Doe",
                "",
                "123abc",      // invalid phone
                "password123",
                callback
        );

        verify(callback).onComplete(captor.capture());
        ServiceResult<User> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Invalid phone format", result.message);
        assertEquals("phone", result.field);
    }

    @Test
    void register_missingName() {

        @SuppressWarnings("unchecked")
        ServiceCallback<User> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<User>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        registrationService.register(
                "",
                "user@example.com",
                "",
                "password123",
                callback
        );

        verify(callback).onComplete(captor.capture());
        ServiceResult<User> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Name required", result.message);
        assertEquals("name", result.field);
    }

    @Test
    void register_missingPassword() {

        @SuppressWarnings("unchecked")
        ServiceCallback<User> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<User>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        registrationService.register(
                "John Doe",
                "user@example.com",
                "",
                "",
                callback
        );


        verify(callback).onComplete(captor.capture());
        ServiceResult<User> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Password required", result.message);
        assertEquals("password", result.field);
    }

    @Test
    void register_repositoryFails() {

        @SuppressWarnings("unchecked")
        ServiceCallback<User> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<User>> captor = ArgumentCaptor.forClass(ServiceResult.class);


        doAnswer(invocation -> {
            RepositoryCallback<User> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(null, "Registration failed"); // repo failure message
            return null;
        }).when(mockRepo).save(any(User.class), any());


        registrationService.register(
                "John Doe",
                "user@example.com",
                "",
                "password123",
                callback
        );


        verify(callback).onComplete(captor.capture());
        ServiceResult<User> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Registration failed", result.message);
        assertEquals("repository", result.field);
    }

    @Test
    public void register_validData() {
        @SuppressWarnings("unchecked")
        ServiceCallback<User> callback = mock(ServiceCallback.class);

        ArgumentCaptor<ServiceResult<User>> captor =
                ArgumentCaptor.forClass((Class) ServiceResult.class);

        doAnswer(invocation -> {
            User userArg = invocation.getArgument(0, User.class);
            RepositoryCallback<Boolean> repoCallback = invocation.getArgument(1);

            userArg.setRole("CUSTOMER");

            repoCallback.onComplete(true, null);

            return null;
        }).when(mockRepo).save(any(User.class), any());

        registrationService.register(
                "John Doe",
                "user@example.com",
                "1234567890",
                "password123",
                callback
        );

        verify(callback).onComplete(captor.capture());
        ServiceResult<User> result = captor.getValue();

        assertTrue(result.success);
        assertNotNull(result.data);
        assertEquals("John Doe", result.data.getName());
        assertEquals("user@example.com", result.data.getEmail());
        assertEquals("1234567890", result.data.getPhone());
        assertEquals("CUSTOMER", result.data.getRole());
    }
}