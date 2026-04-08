package com.example.ticketnoob.service;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.ticketnoob.model.User;
import com.example.ticketnoob.repository.UserRepository;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class RegistrationFunctionalTest {

    private static final long TIMEOUT_SECONDS = 5L;

    @Test
    public void register_duplicateEmail_fails() throws InterruptedException {
        UserRepository userRepository = new UserRepository();
        RegistrationService registrationService = new RegistrationService(userRepository);

        String unique = UUID.randomUUID().toString().substring(0, 8);
        String email = "duplicate" + unique + "@example.com";

        // First registration (should succeed)
        AtomicReference<ServiceResult<User>> firstRef = new AtomicReference<>();
        CountDownLatch firstLatch = new CountDownLatch(1);

        registrationService.register(
                "First User",
                email,
                "",
                "TestPass123",
                result -> {
                    firstRef.set(result);
                    firstLatch.countDown();
                }
        );

        assertTrue(firstLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        ServiceResult<User> firstResult = firstRef.get();

        assertNotNull(firstResult);
        assertTrue(firstResult.success);
        assertNotNull(firstResult.data);

        String userId = firstResult.data.getId();

        // Second registration (should fail)
        AtomicReference<ServiceResult<User>> secondRef = new AtomicReference<>();
        CountDownLatch secondLatch = new CountDownLatch(1);

        registrationService.register(
                "Second User",
                email,
                "",
                "AnotherPass123",
                result -> {
                    secondRef.set(result);
                    secondLatch.countDown();
                }
        );

        assertTrue(secondLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        ServiceResult<User> secondResult = secondRef.get();
        assertNotNull(secondResult);
        assertFalse(secondResult.success);
        assertNull(secondResult.data);
        assertEquals("Email already exists", secondResult.message);

        // CLEANUP
        CountDownLatch cleanupLatch = new CountDownLatch(1);
        userRepository.deleteUser(userId, (deleted, error) -> cleanupLatch.countDown());

        assertTrue(cleanupLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    public void register_duplicatePhone_fails() throws InterruptedException {
        UserRepository userRepository = new UserRepository();
        RegistrationService registrationService = new RegistrationService(userRepository);

        String uniqueDigits = UUID.randomUUID().toString().replaceAll("[^0-9]", "");
        if (uniqueDigits.length() < 10) {
            uniqueDigits += "1234567890";
        }
        String phone = uniqueDigits.substring(0, 10);

        // First registration
        AtomicReference<ServiceResult<User>> firstRef = new AtomicReference<>();
        CountDownLatch firstLatch = new CountDownLatch(1);

        registrationService.register(
                "First User",
                "",
                phone,
                "TestPass123",
                result -> {
                    firstRef.set(result);
                    firstLatch.countDown();
                }
        );

        assertTrue(firstLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        ServiceResult<User> firstResult = firstRef.get();

        assertNotNull(firstResult);
        assertTrue(firstResult.success);
        assertNotNull(firstResult.data);

        String userId = firstResult.data.getId();

        // Second registration (same phone)
        AtomicReference<ServiceResult<User>> secondRef = new AtomicReference<>();
        CountDownLatch secondLatch = new CountDownLatch(1);

        registrationService.register(
                "Second User",
                "",
                phone,
                "AnotherPass123",
                result -> {
                    secondRef.set(result);
                    secondLatch.countDown();
                }
        );

        assertTrue(secondLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        ServiceResult<User> secondResult = secondRef.get();
        assertNotNull(secondResult);
        assertFalse(secondResult.success);
        assertNull(secondResult.data);
        assertEquals("Phone already exists", secondResult.message);

        // CLEANUP
        CountDownLatch cleanupLatch = new CountDownLatch(1);
        userRepository.deleteUser(userId, (deleted, error) -> cleanupLatch.countDown());

        assertTrue(cleanupLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }
}