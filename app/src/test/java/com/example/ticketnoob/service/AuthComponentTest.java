package com.example.ticketnoob.service;

import com.example.ticketnoob.model.User;
import com.example.ticketnoob.repository.RepositoryCallback;
import com.example.ticketnoob.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Component Test: Validates interaction between RegistrationService and LoginService
 * with mocked repository
 */
public class AuthComponentTest {

    private UserRepository mockRepo;

    private LoginService loginService;
    private RegistrationService registrationService;

    @BeforeEach
    public void setUp() {
        mockRepo = mock(UserRepository.class);
        loginService = new LoginService(mockRepo);
        registrationService = new RegistrationService(mockRepo);
    }

    @Test
    public void testRegisterThenLogin_SuccessFlow() {

        String name = "Integration User";
        String email = "integration@example.com";
        String password = "password123";
        @SuppressWarnings("unchecked")
        ServiceCallback<User> registerCallback = mock(ServiceCallback.class);

        ArgumentCaptor<ServiceResult<User>> registerCaptor =
                ArgumentCaptor.forClass((Class) ServiceResult.class);

        doAnswer(invocation -> {
            User user = invocation.getArgument(0, User.class);
            RepositoryCallback<Boolean> repoCallback = invocation.getArgument(1);

            user.setRole("CUSTOMER");

            // FIX: correct callback signature
            repoCallback.onComplete(true, null);

            return null;
        }).when(mockRepo).save(any(User.class), any());

        // Call register
        registrationService.register(
                name,
                email,
                "",
                password,
                registerCallback
        );

        verify(registerCallback).onComplete(registerCaptor.capture());
        ServiceResult<User> registerResult = registerCaptor.getValue();

        assertTrue(registerResult.success);
        assertNotNull(registerResult.data);

        User registeredUser = registerResult.data;

        @SuppressWarnings("unchecked")
        ServiceCallback<User> loginCallback = mock(ServiceCallback.class);

        ArgumentCaptor<ServiceResult<User>> loginCaptor =
                ArgumentCaptor.forClass((Class) ServiceResult.class);

        // Mock authenticate (this one IS correct — authenticate returns User)
        doAnswer(invocation -> {
            RepositoryCallback<User> repoCallback = invocation.getArgument(2);
            repoCallback.onComplete(registeredUser, null);
            return null;
        }).when(mockRepo).authenticate(eq(email), eq(password), any());

        // Call login
        loginService.login(email, password, loginCallback);

        verify(loginCallback).onComplete(loginCaptor.capture());
        ServiceResult<User> loginResult = loginCaptor.getValue();

        assertTrue(loginResult.success);
        assertNotNull(loginResult.data);
        assertEquals(registeredUser.getEmail(), loginResult.data.getEmail());

        verify(mockRepo).save(any(User.class), any());
        verify(mockRepo).authenticate(eq(email), eq(password), any());
    }

    @Test
    public void testRegister_PhoneSuccessFlow() {
        String name = "Phone User";
        String phone = "1234567890";
        String password = "password123";

        @SuppressWarnings("unchecked")
        ServiceCallback<User> registerCallback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<User>> captor = ArgumentCaptor.forClass((Class) ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Boolean> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(true, null);
            return null;
        }).when(mockRepo).save(any(User.class), any());

        registrationService.register(name, "", phone, password, registerCallback);

        verify(registerCallback).onComplete(captor.capture());
        ServiceResult<User> result = captor.getValue();

        assertTrue(result.success);
        assertNotNull(result.data);

        User registeredUser = result.data;

        @SuppressWarnings("unchecked")
        ServiceCallback<User> loginCallback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<User>> loginCaptor = ArgumentCaptor.forClass((Class) ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<User> repoCallback = invocation.getArgument(2);
            repoCallback.onComplete(registeredUser, null);
            return null;
        }).when(mockRepo).authenticate(eq(phone), eq(password), any());

        loginService.login(phone, password, loginCallback);

        verify(mockRepo).authenticate(eq(phone), eq(password), any());

        verify(loginCallback).onComplete(loginCaptor.capture());
        assertTrue(loginCaptor.getValue().success);
        assertEquals(phone, loginCaptor.getValue().data.getPhone());
    }

    @Test
    public void testRegister_InvalidEmailFailure() {
        String invalidEmail = "not-an-email";

        @SuppressWarnings("unchecked")
        ServiceCallback<User> registerCallback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<User>> captor = ArgumentCaptor.forClass((Class) ServiceResult.class);

        registrationService.register("User", invalidEmail, "", "pass123", registerCallback);

        verify(registerCallback).onComplete(captor.capture());
        ServiceResult<User> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Invalid email format", result.message);

        verify(mockRepo, never()).save(any(), any());
    }

    @Test
    public void testRegister_InvalidPhoneFailure() {
        String invalidPhone = "123-abc";

        @SuppressWarnings("unchecked")
        ServiceCallback<User> registerCallback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<User>> captor = ArgumentCaptor.forClass((Class) ServiceResult.class);

        registrationService.register("User", "", invalidPhone, "pass123", registerCallback);

        verify(registerCallback).onComplete(captor.capture());
        ServiceResult<User> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Invalid phone format", result.message);

        verify(mockRepo, never()).save(any(), any());
    }

    @Test
    public void testRegister_NoContactMethodFailure() {
        String name = "No Contact User";
        String email = "";
        String phone = "";
        String password = "password123";

        @SuppressWarnings("unchecked")
        ServiceCallback<User> registerCallback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<User>> captor = ArgumentCaptor.forClass((Class) ServiceResult.class);

        registrationService.register(name, email, phone, password, registerCallback);

        verify(registerCallback).onComplete(captor.capture());
        ServiceResult<User> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Provide email OR phone", result.message);

        verify(mockRepo, never()).save(any(), any());
    }

    @Test
    public void testRegister_NoPasswordFailure() {
        String name = "No Password User";
        String email = "valid@example.com";
        String phone = "";
        String password = "";
        @SuppressWarnings("unchecked")
        ServiceCallback<User> registerCallback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<User>> captor = ArgumentCaptor.forClass((Class) ServiceResult.class);

        registrationService.register(name, email, phone, password, registerCallback);

        verify(registerCallback).onComplete(captor.capture());
        ServiceResult<User> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("Password required", result.message);

        verify(mockRepo, never()).save(any(), any());
    }

    @Test
    public void testLogin_MultipleAttempts_RepoCalledEachTime() {

        String email = "test@example.com";
        String password1 = "wrongpassword";
        String password2 = "correctpassword";
        String hash1 = BCrypt.hashpw(password2, BCrypt.gensalt());
        User mockUser = new User("1", "Test User", email, null, hash1, "CUSTOMER");

        // Mock authenticate

        doAnswer(invocation -> {

            String passedEmail = invocation.getArgument(0);
            String passedPassword = invocation.getArgument(1);
            RepositoryCallback<User> repoCallback = invocation.getArgument(2);

            if (BCrypt.checkpw(passedPassword, hash1)) {
                repoCallback.onComplete(mockUser, null);
            } else {
                repoCallback.onComplete(null, "Invalid credentials");
            }
            return null;
        }).when(mockRepo).authenticate(anyString(), anyString(), any());

        // First attempt (fails)
        @SuppressWarnings("unchecked")
        ServiceCallback<User> callback1 = mock(ServiceCallback.class);

        ArgumentCaptor<ServiceResult<User>> captor1 =
                ArgumentCaptor.forClass(ServiceResult.class);

        loginService.login(email, password1, callback1);

        verify(callback1).onComplete(captor1.capture());
        ServiceResult<User> result1 = captor1.getValue();

        assertFalse(result1.success, "First login should fail");

        // Second attempt (succeeds)
        @SuppressWarnings("unchecked")
        ServiceCallback<User> callback2 = mock(ServiceCallback.class);

        ArgumentCaptor<ServiceResult<User>> captor2 =
                ArgumentCaptor.forClass(ServiceResult.class);

        loginService.login(email, password2, callback2);

        verify(callback2).onComplete(captor2.capture());
        ServiceResult<User> result2 = captor2.getValue();

        assertTrue(result2.success, "Second login should succeed");
        assertNotNull(result2.data);
        assertEquals(email, result2.data.getEmail());


        // Verify repository interactions

        verify(mockRepo, times(1)).authenticate(eq(email), eq(password1), any());
        verify(mockRepo, times(1)).authenticate(eq(email), eq(password2), any());
        verify(mockRepo, times(2)).authenticate(anyString(), anyString(), any());
    }

    @Test
    public void testRegister_DuplicateUserFailure() {
        String email = "existing@example.com";

        @SuppressWarnings("unchecked")
        ServiceCallback<User> registerCallback = mock(ServiceCallback.class);
        ArgumentCaptor<ServiceResult<User>> captor = ArgumentCaptor.forClass((Class) ServiceResult.class);

        doAnswer(invocation -> {
            RepositoryCallback<Boolean> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(false, "User already exists");
            return null;
        }).when(mockRepo).save(any(User.class), any());

        registrationService.register("New User", email, "", "password123", registerCallback);

        verify(registerCallback).onComplete(captor.capture());
        ServiceResult<User> result = captor.getValue();

        assertFalse(result.success);
        assertEquals("User already exists", result.message);
    }

    @Test
    public void testRegister_PasswordIsHashedBeforeSaving() {
        String plainPassword = "securePassword123";
        String name = "Security User";
        String email = "security@example.com";

        @SuppressWarnings("unchecked")
        ServiceCallback<User> registerCallback = mock(ServiceCallback.class);

        doAnswer(invocation -> {
            RepositoryCallback<Boolean> repoCallback = invocation.getArgument(1);
            repoCallback.onComplete(true, null);
            return null;
        }).when(mockRepo).save(any(User.class), any());

        registrationService.register(name, email, "", plainPassword, registerCallback);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(mockRepo).save(userCaptor.capture(), any());

        User capturedUser = userCaptor.getValue();
        String storedPassword = capturedUser.getPassword();

        // Verification A: Ensure the stored password is NOT the plain text
        assertNotEquals("Password should not be stored in plain text!",
                plainPassword, storedPassword);

        // Verification B: Ensure it follows the BCrypt format
        assertTrue(storedPassword.startsWith("$2"));

        // Verification C: Use BCrypt to verify the plain text matches the captured hash
        assertTrue(BCrypt.checkpw(plainPassword, storedPassword));
    }

}