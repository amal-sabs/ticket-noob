package com.example.ticketnoob.service;

import com.example.ticketnoob.model.User;
import com.example.ticketnoob.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
    void register_missingEmailAndPhone() {
        ServiceResult<User> result = registrationService.register("John Doe", "", "", "password123");

        assertFalse(result.success);
        assertEquals("Provide email OR phone", result.message);
        assertEquals("email_phone", result.field);
    }

    @Test
    void register_invalidEmailFormat() {
        ServiceResult<User> result = registrationService.register("John Doe", "invalid-email", "", "password123");

        assertFalse(result.success);
        assertEquals("Invalid email format", result.message);
        assertEquals("email", result.field);
    }

    @Test
    void register_invalidPhoneFormat() {
        ServiceResult<User> result = registrationService.register("John Doe", "", "123abc", "password123");

        assertFalse(result.success);
        assertEquals("Invalid phone format", result.message);
        assertEquals("phone", result.field);
    }

    @Test
    void register_missingName() {
        ServiceResult<User> result = registrationService.register("", "user@example.com", "", "password123");

        assertFalse(result.success);
        assertEquals("Name required", result.message);
        assertEquals("name", result.field);
    }

    @Test
    void register_missingPassword() {
        ServiceResult<User> result = registrationService.register("John Doe", "user@example.com", "", "");

        assertFalse(result.success);
        assertEquals("Password required", result.message);
        assertEquals("password", result.field);
    }

    @Test
    void register_repositoryFails() {
        when(mockRepo.save(any(User.class))).thenThrow(new RuntimeException("DB error"));

        ServiceResult<User> result = registrationService.register("John Doe", "user@example.com", "", "password123");

        assertFalse(result.success);
        assertEquals("Registration failed", result.message);
        assertEquals("repository", result.field);
    }

    @Test
    void register_validData() {
        when(mockRepo.save(any(User.class))).thenReturn(true);

        ServiceResult<User> result = registrationService.register("John Doe", "user@example.com", "1234567890", "password123");

        assertTrue(result.success);
        assertNotNull(result.data);
        assertEquals("John Doe", result.data.getName());
        assertEquals("user@example.com", result.data.getEmail());
        assertEquals("1234567890", result.data.getPhone());
        assertEquals("CUSTOMER", result.data.getRole());
    }
}