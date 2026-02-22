package com.example.ticketnoob.service;

import com.example.ticketnoob.model.User;
import com.example.ticketnoob.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
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
        ServiceResult<User> result = loginService.login("", "password123");

        assertFalse(result.success);
        assertEquals("Please provide email or phone number", result.message);
        assertEquals("emailOrPhone", result.field);
    }

    @Test
    void login_emptyPassword() {
        ServiceResult<User> result = loginService.login("user@example.com", "");

        assertFalse(result.success);
        assertEquals("Please provide a password", result.message);
        assertEquals("password", result.field);
    }

    @Test
    void login_invalidCredentials() {
        ServiceResult<User> result = loginService.login("user@example.com", "wrongpass");

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

        when(mockRepo.authenticate("user@example.com", "correctpass")).thenReturn(mockUser);

        ServiceResult<User> result = loginService.login("user@example.com", "correctpass");

        assertTrue(result.success);
        assertNotNull(result.data);
        assertEquals("123", result.data.getId());
        assertEquals("John Doe", result.data.getName());
        assertEquals("user@example.com", result.data.getEmail());
    }
}