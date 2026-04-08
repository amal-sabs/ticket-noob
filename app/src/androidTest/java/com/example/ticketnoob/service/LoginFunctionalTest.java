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
public class LoginFunctionalTest {

    private static final long TIMEOUT_SECONDS = 15L;

    private UserRepository userRepository;
    private RegistrationService registrationService;
    private LoginService loginService;
    private final List<String> createdUserIds = new ArrayList<>();

    @Before
    public void setUp() {
        userRepository = new UserRepository();
        registrationService = new RegistrationService(userRepository);
        loginService = new LoginService(userRepository);
    }

    @After
    public void tearDown() throws InterruptedException {
        for (String userId : createdUserIds) {
            CountDownLatch cleanupLatch = new CountDownLatch(1);

            userRepository.deleteUser(userId, (deleted, error) -> cleanupLatch.countDown());

            cleanupLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        createdUserIds.clear();
    }

    @Test
    public void login_withValidCredentials_succeeds() throws InterruptedException {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String email = "validlogin" + unique + "@example.com";
        String password = "CorrectPass123";

        ServiceResult<User> registerResult = registerUser("Test User", email, "", password);

        assertNotNull("Register result is null", registerResult);
        assertTrue("Registration failed: " + registerResult.message, registerResult.success);
        assertNotNull("Registered user is null", registerResult.data);
        assertNotNull("Registered user ID is null", registerResult.data.getId());

        createdUserIds.add(registerResult.data.getId());

        ServiceResult<User> loginResult = loginUser(email, password);

        assertNotNull("Login result is null", loginResult);
        assertTrue("Login failed: " + loginResult.message, loginResult.success);
        assertNotNull("Logged-in user is null", loginResult.data);
        assertEquals("Email mismatch", email, loginResult.data.getEmail());
        assertEquals("User ID mismatch", registerResult.data.getId(), loginResult.data.getId());
    }

    @Test
    public void login_withWrongPassword_fails() throws InterruptedException {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String email = "wrongpass" + unique + "@example.com";

        ServiceResult<User> registerResult = registerUser("Test User", email, "", "CorrectPass123");

        assertNotNull(registerResult);
        assertTrue(registerResult.success);
        assertNotNull(registerResult.data);

        createdUserIds.add(registerResult.data.getId());

        ServiceResult<User> loginResult = loginUser(email, "WrongPass123");

        assertNotNull(loginResult);
        assertFalse(loginResult.success);
        assertNull(loginResult.data);
        assertEquals("Invalid credentials", loginResult.message);
    }

    @Test
    public void login_withNonexistentEmail_fails() throws InterruptedException {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String email = "nouser" + unique + "@example.com";

        ServiceResult<User> result = loginUser(email, "SomePass123");

        assertNotNull(result);
        assertFalse(result.success);
        assertNull(result.data);
        assertEquals("Invalid credentials", result.message);
    }

    @Test
    public void login_withNonexistentPhone_fails() throws InterruptedException {
        String phone = "9999999999";

        ServiceResult<User> result = loginUser(phone, "SomePass123");

        assertNotNull(result);
        assertFalse(result.success);
        assertNull(result.data);
        assertEquals("Invalid credentials", result.message);
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

    private ServiceResult<User> loginUser(
            String emailOrPhone,
            String password
    ) throws InterruptedException {
        AtomicReference<ServiceResult<User>> loginRef = new AtomicReference<>();
        CountDownLatch loginLatch = new CountDownLatch(1);

        loginService.login(emailOrPhone, password, result -> {
            loginRef.set(result);
            loginLatch.countDown();
        });

        assertTrue("Login timed out",
                loginLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        return loginRef.get();
    }
}