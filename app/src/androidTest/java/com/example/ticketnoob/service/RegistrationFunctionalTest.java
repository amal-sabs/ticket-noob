package com.example.ticketnoob.service;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.ticketnoob.model.User;
import com.example.ticketnoob.repository.UserRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class RegistrationFunctionalTest {

    private static final long TIMEOUT_SECONDS = 5L;

    private UserRepository userRepository;
    private RegistrationService registrationService;
    private final List<String> createdUserIds = new ArrayList<>();

    @Before
    public void setUp() {
        userRepository = new UserRepository();
        registrationService = new RegistrationService(userRepository);
    }

    @After
    public void tearDown() throws InterruptedException {
        for (String userId : createdUserIds) {
            CountDownLatch cleanupLatch = new CountDownLatch(1);

            userRepository.deleteUser(userId, (deleted, error) -> {
                cleanupLatch.countDown();
            });

            cleanupLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        createdUserIds.clear();
    }

    @Test
    public void register_withValidEmail_succeeds() throws InterruptedException {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String email = "validregister" + unique + "@example.com";
        String password = "TestPass123";

        ServiceResult<User> result = registerUser("John Doe", email, "", password);

        assertNotNull("Register result is null", result);
        assertTrue("Registration failed: " + result.message, result.success);
        assertNotNull("Registered user is null", result.data);
        assertNotNull("User ID should not be null", result.data.getId());
        assertEquals("John Doe", result.data.getName());
        assertEquals(email, result.data.getEmail());
        assertEquals("CUSTOMER", result.data.getRole());

        createdUserIds.add(result.data.getId());
    }

    @Test
    public void register_duplicateEmail_fails() throws InterruptedException {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String email = "duplicate" + unique + "@example.com";

        ServiceResult<User> firstResult = registerUser("First User", email, "", "TestPass123");

        assertNotNull(firstResult);
        assertTrue(firstResult.success);
        assertNotNull(firstResult.data);

        createdUserIds.add(firstResult.data.getId());

        ServiceResult<User> secondResult = registerUser("Second User", email, "", "AnotherPass123");

        assertNotNull(secondResult);
        assertFalse(secondResult.success);
        assertNull(secondResult.data);
        assertEquals("Email already exists", secondResult.message);
    }

    @Test
    public void register_duplicatePhone_fails() throws InterruptedException {
        String uniqueDigits = UUID.randomUUID().toString().replaceAll("[^0-9]", "");
        if (uniqueDigits.length() < 10) {
            uniqueDigits += "1234567890";
        }
        String phone = uniqueDigits.substring(0, 10);

        ServiceResult<User> firstResult = registerUser("First User", "", phone, "TestPass123");

        assertNotNull(firstResult);
        assertTrue(firstResult.success);
        assertNotNull(firstResult.data);

        createdUserIds.add(firstResult.data.getId());

        ServiceResult<User> secondResult = registerUser("Second User", "", phone, "AnotherPass123");

        assertNotNull(secondResult);
        assertFalse(secondResult.success);
        assertNull(secondResult.data);
        assertEquals("Phone already exists", secondResult.message);
    }

    private ServiceResult<User> registerUser(
            String name,
            String email,
            String phone,
            String password
    ) throws InterruptedException {
        AtomicReference<ServiceResult<User>> registerRef = new AtomicReference<>();
        CountDownLatch registerLatch = new CountDownLatch(1);

        registrationService.register(
                name,
                email,
                phone,
                password,
                result -> {
                    registerRef.set(result);
                    registerLatch.countDown();
                }
        );

        assertTrue("Register timed out",
                registerLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        return registerRef.get();
    }
}