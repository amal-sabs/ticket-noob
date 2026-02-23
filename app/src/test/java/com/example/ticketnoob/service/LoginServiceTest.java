package com.example.ticketnoob.service;

import com.example.ticketnoob.model.User;
import com.example.ticketnoob.repository.RepositoryCallback;
import com.example.ticketnoob.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginServiceTest {

    private LoginService loginService;
    private UserRepository mockRepo;

    @BeforeEach
    void setUp() {
        mockRepo = mock(UserRepository.class);
        loginService = new LoginService(mockRepo);
    }

    @Test
    void login_emptyEmailOrPhone() {
        @SuppressWarnings("unchecked")
        ServiceCallback<User> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<User>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        loginService.login("", "password123", callback);

        // Capture the ServiceResult passed to the callback
        verify(callback).onComplete(captor.capture());
        ServiceResult<User> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Provide email or phone", result.message);
        assertEquals("emailOrPhone", result.field);
    }

    @Test
    void login_emptyPassword() {
        @SuppressWarnings("unchecked")
        ServiceCallback<User> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<User>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        loginService.login("user@example.com", "", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<User> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Provide password", result.message);
        assertEquals("password", result.field);
    }

    @Test
    void login_invalidCredentials() {
        @SuppressWarnings("unchecked")
        ServiceCallback<User> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<User>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<User> repoCallback = invocation.getArgument(2);
            repoCallback.onComplete(null, "Invalid credentials"); // simulate repo response
            return null;
        }).when(mockRepo).authenticate(eq("user@example.com"), eq("wrongpass"), any());


        loginService.login("user@example.com", "wrongpass", callback);

        verify(callback).onComplete(captor.capture());
        ServiceResult<User> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Invalid credentials", result.message);
        assertEquals("credentials", result.field);
    }

    @Test
    void login_validCredentials() {
        User mockUser = new User();
        mockUser.setId("123");
        mockUser.setName("John Doe");
        mockUser.setEmail("user@example.com");
        mockUser.setPassword("correctpass");

        @SuppressWarnings("unchecked")
        ServiceCallback<User> callback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<User>> captor = ArgumentCaptor.forClass(ServiceResult.class);

        // Mock repository to call the callback with the user
        doAnswer(invocation -> {
            RepositoryCallback<User> repoCallback = invocation.getArgument(2);
            repoCallback.onComplete(mockUser, null);
            return null;
        }).when(mockRepo).authenticate(eq("user@example.com"), eq("correctpass"), any());

        loginService.login("user@example.com", "correctpass", callback);


        verify(callback).onComplete(captor.capture());
        ServiceResult<User> result = captor.getValue();

        assertTrue(result.success);
        assertNotNull(result.data);
        assertEquals("123", result.data.getId());
        assertEquals("John Doe", result.data.getName());
        assertEquals("user@example.com", result.data.getEmail());
    }
}