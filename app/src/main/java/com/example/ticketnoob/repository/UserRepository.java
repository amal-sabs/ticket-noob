package com.example.ticketnoob.repository;

import com.example.ticketapp.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserRepository {

    private final List<User> users = new ArrayList<>();

    public boolean save(User user) {
        if (user == null) return false;

        // Prevent duplicate email
        if (user.getEmail() != null && findByEmail(user.getEmail()) != null) {
            return false;
        }

        // Prevent duplicate phone
        if (user.getPhone() != null && findByPhone(user.getPhone()) != null) {
            return false;
        }

        // Auto-generate ID if missing
        if (user.getId() == null || user.getId().isEmpty()) {
            user.setId(UUID.randomUUID().toString());
        }

        users.add(user);
        return true;
    }

    public User findByEmail(String email) {
        for (User user : users) {
            if (email != null && email.equalsIgnoreCase(user.getEmail())) {
                return user;
            }
        }
        return null;
    }

    public User findByPhone(String phone) {
        for (User user : users) {
            if (phone != null && phone.equals(user.getPhone())) {
                return user;
            }
        }
        return null;
    }

    public User authenticate(String emailOrPhone, String password) {
        for (User user : users) {

            boolean matchesEmail =
                    user.getEmail() != null &&
                            user.getEmail().equalsIgnoreCase(emailOrPhone);

            boolean matchesPhone =
                    user.getPhone() != null &&
                            user.getPhone().equals(emailOrPhone);

            if ((matchesEmail || matchesPhone)
                    && user.getPassword().equals(password)) {
                return user;
            }
        }
        return null;
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    public boolean deleteUser(String userId) {
        return users.removeIf(user -> user.getId().equals(userId));
    }
}
