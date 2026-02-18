package com.example.ticketnoob;


import com.example.ticketnoob.model.User;
import com.example.ticketnoob.repository.UserRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserRepositoryTest {

    @Test
    void authenticate_success_withEmail() {
        UserRepository repo = new UserRepository();
        repo.save(new User("Pam", "pam@mail.com", null, "1234", "CUSTOMER"));

        assertNotNull(repo.authenticate("pam@mail.com", "1234"));
    }

    @Test
    void authenticate_success_withPhone() {
        UserRepository repo = new UserRepository();
        repo.save(new User("Pam", null, "5141234567", "1234", "CUSTOMER"));

        assertNotNull(repo.authenticate("5141234567", "1234"));
    }

    @Test
    void authenticate_fails_wrongPassword() {
        UserRepository repo = new UserRepository();
        repo.save(new User("Pam", "pam@mail.com", null, "1234", "CUSTOMER"));

        assertNull(repo.authenticate("pam@mail.com", "wrong"));
    }

    @Test
    void authenticate_fails_userNotFound() {
        UserRepository repo = new UserRepository();
        repo.save(new User("Pam", "pam@mail.com", null, "1234", "CUSTOMER"));

        assertNull(repo.authenticate("other@mail.com", "1234"));
    }
}
