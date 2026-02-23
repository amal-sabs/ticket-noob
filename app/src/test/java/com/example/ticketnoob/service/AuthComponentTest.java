//package com.example.ticketnoob.service;
//
//import com.example.ticketnoob.model.User;
//import com.example.ticketnoob.repository.UserRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
///**
// * Component Test: Validates interaction between RegistrationService and LoginService
// * with mocked repository
// */
//public class AuthComponentTest {
//
//    private UserRepository mockRepo;
//
//    private LoginService loginService;
//    private RegistrationService registrationService;
//
//    @BeforeEach
//    public void setUp() {
//        mockRepo = mock(UserRepository.class);
//        loginService = new LoginService(mockRepo);
//        registrationService = new RegistrationService(mockRepo);
//    }
//
//    @Test
//    // happy path flow
//    public void testRegisterThenLogin_SuccessFlow() {
//        String name = "Integration User";
//        String email = "integration@example.com";
//        String password = "password123";
//
//        // Mock registration success
//        when(mockRepo.save(any(User.class))).thenReturn(true);
//
//        // Register
//        ServiceResult<User> registerResult = registrationService.register(name, email, "", password);
//
//        // Assert registration
//        assertTrue(registerResult.success, "Registration should succeed");
//        assertNotNull(registerResult.data, "Registered user should not be null");
//
//        // Mock login with the registered user
//        User registeredUser = registerResult.data;
//        when(mockRepo.authenticate(email, password)).thenReturn(registeredUser);
//
//        // Login
//        ServiceResult<User> loginResult = loginService.login(email, password);
//
//        // Assert login
//        assertTrue(loginResult.success, "Login should succeed");
//        assertNotNull(loginResult.data, "Logged in user should not be null");
//        assertEquals(registeredUser.getEmail(), loginResult.data.getEmail(), "Users should have same email");
//
//        // Verify repository interactions
//        verify(mockRepo, times(1)).save(any(User.class));
//        verify(mockRepo, times(1)).authenticate(email, password);
//    }
//
//    @Test
//    public void testLogin_MultipleAttempts_RepoCalledEachTime() {
//        String email = "test@example.com";
//        String password1 = "wrongpassword";
//        String password2 = "correctpassword";
//        User mockUser = new User("1", "Test User", email, null, password2, "CUSTOMER");
//
//        when(mockRepo.authenticate(email, password1)).thenReturn(null);
//        when(mockRepo.authenticate(email, password2)).thenReturn(mockUser);
//
//        // First attempt (fails)
//        ServiceResult<User> result1 = loginService.login(email, password1);
//        assertFalse(result1.success, "First login should fail");
//
//        // Second attempt (succeeds)
//        ServiceResult<User> result2 = loginService.login(email, password2);
//        assertTrue(result2.success, "Second login should succeed");
//
//        // Verify repository interactions
//        verify(mockRepo, times(1)).authenticate(email, password1);
//        verify(mockRepo, times(1)).authenticate(email, password2);
//        verify(mockRepo, times(2)).authenticate(anyString(), anyString());
//    }
//}