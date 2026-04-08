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
public class LoginFunctionalTest {

    private static final long TIMEOUT_SECONDS = 5L;

    @Test
    public void login_withWrongPassword_fails() throws InterruptedException {
        UserRepository userRepository = new UserRepository();
        RegistrationService registrationService = new RegistrationService(userRepository);
        LoginService loginService = new LoginService(userRepository);

        String unique = UUID.randomUUID().toString().substring(0, 8);
        String email = "wrongpass" + unique + "@example.com";

        // Register user
        AtomicReference<ServiceResult<User>> registerRef = new AtomicReference<>();
        CountDownLatch registerLatch = new CountDownLatch(1);

        registrationService.register(
                "Test User",
                email,
                "",
                "CorrectPass123",
                result -> {
                    registerRef.set(result);
                    registerLatch.countDown();
                }
        );

        assertTrue(registerLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        ServiceResult<User> registerResult = registerRef.get();

        assertNotNull(registerResult);
        assertTrue(registerResult.success);
        assertNotNull(registerResult.data);

        String userId = registerResult.data.getId();

        // Try wrong password
        AtomicReference<ServiceResult<User>> loginRef = new AtomicReference<>();
        CountDownLatch loginLatch = new CountDownLatch(1);

        loginService.login(email, "WrongPass123", result -> {
            loginRef.set(result);
            loginLatch.countDown();
        });

        assertTrue(loginLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        ServiceResult<User> loginResult = loginRef.get();
        assertNotNull(loginResult);
        assertFalse(loginResult.success);
        assertNull(loginResult.data);
        assertEquals("Invalid credentials", loginResult.message);

        // CLEANUP
        CountDownLatch cleanupLatch = new CountDownLatch(1);
        userRepository.deleteUser(userId, (deleted, error) -> cleanupLatch.countDown());

        assertTrue(cleanupLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    public void login_withNonexistentEmail_fails() throws InterruptedException {
        UserRepository userRepository = new UserRepository();
        LoginService loginService = new LoginService(userRepository);

        String unique = UUID.randomUUID().toString().substring(0, 8);
        String email = "nouser" + unique + "@example.com";

        AtomicReference<ServiceResult<User>> loginRef = new AtomicReference<>();
        CountDownLatch loginLatch = new CountDownLatch(1);

        loginService.login(email, "SomePass123", result -> {
            loginRef.set(result);
            loginLatch.countDown();
        });

        assertTrue(loginLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        ServiceResult<User> result = loginRef.get();
        assertNotNull(result);
        assertFalse(result.success);
        assertNull(result.data);
        assertEquals("Invalid credentials", result.message);
    }

    @Test
    public void login_withNonexistentPhone_fails() throws InterruptedException {
        UserRepository userRepository = new UserRepository();
        LoginService loginService = new LoginService(userRepository);

        String uniqueDigits = UUID.randomUUID().toString().replaceAll("[^0-9]", "");
        if (uniqueDigits.length() < 10) {
            uniqueDigits += "1234567890";
        }
        String phone = uniqueDigits.substring(0, 10);

        AtomicReference<ServiceResult<User>> loginRef = new AtomicReference<>();
        CountDownLatch loginLatch = new CountDownLatch(1);

        loginService.login(phone, "SomePass123", result -> {
            loginRef.set(result);
            loginLatch.countDown();
        });

        assertTrue(loginLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        ServiceResult<User> result = loginRef.get();
        assertNotNull(result);
        assertFalse(result.success);
        assertNull(result.data);
        assertEquals("Invalid credentials", result.message);
    }
}