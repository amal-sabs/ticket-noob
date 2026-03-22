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
public class RegistrationFlowFunctionalTest {

    private static final long TIMEOUT_SECONDS = 5L;

    @Test
    public void register_then_login_success() throws InterruptedException {
        UserRepository userRepository = new UserRepository();
        RegistrationService registrationService = new RegistrationService(userRepository);
        LoginService loginService = new LoginService(userRepository);

        // test data
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String email = "functionalTest" + unique + "@example.com";
        String password = "TestPass123";

        // REGISTER
        AtomicReference<ServiceResult<User>> registerRef = new AtomicReference<>();
        CountDownLatch registerLatch = new CountDownLatch(1);

        registrationService.register(
                "Test User",
                email,
                "",
                password,
                result -> {
                    registerRef.set(result);
                    registerLatch.countDown();
                }
        );

        assertTrue("Register timed out",
                registerLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        ServiceResult<User> registerResult = registerRef.get();
        assertNotNull("Register result is null", registerResult);
        assertTrue("Registration failed: " + registerResult.message, registerResult.success);
        assertNotNull("Registered user is null", registerResult.data);
        assertNotNull("User ID should not be null", registerResult.data.getId());

        String userId = registerResult.data.getId();

        // LOGIN
        AtomicReference<ServiceResult<User>> loginRef = new AtomicReference<>();
        CountDownLatch loginLatch = new CountDownLatch(1);

        loginService.login(email, password, result -> {
            loginRef.set(result);
            loginLatch.countDown();
        });

        assertTrue("Login timed out",
                loginLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        ServiceResult<User> loginResult = loginRef.get();
        assertNotNull("Login result is null", loginResult);
        assertTrue("Login failed: " + loginResult.message, loginResult.success);
        assertNotNull("Logged-in user is null", loginResult.data);

        assertEquals("User ID mismatch between register and login",
                userId, loginResult.data.getId());

        // CLEANUP
        CountDownLatch cleanupLatch = new CountDownLatch(1);

        userRepository.deleteUser(userId, (deleted, error) -> {
            cleanupLatch.countDown();
        });

        assertTrue("Cleanup timed out",
                cleanupLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }
}
