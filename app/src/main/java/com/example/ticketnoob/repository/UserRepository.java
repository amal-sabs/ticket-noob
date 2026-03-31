package com.example.ticketnoob.repository;

import com.example.ticketnoob.model.User;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.mindrot.jbcrypt.BCrypt;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserRepository {

    private final FirebaseFirestore database;

    public UserRepository() {
        this.database = FirebaseFirestore.getInstance();
    }

    public void save(User user, RepositoryCallback<Boolean> callback) {

        if (user == null) {
            callback.onComplete(false, "User is null");
            return;
        }

        if (user.getId() == null || user.getId().isEmpty()) {
            user.setId(UUID.randomUUID().toString());
        }

        boolean hasEmail = user.getEmail() != null && !user.getEmail().isEmpty();
        boolean hasPhone = user.getPhone() != null && !user.getPhone().isEmpty();

        if (!hasEmail && !hasPhone) {
            callback.onComplete(false, "Email or phone required");
            return;
        }

        Query query;

        if (hasEmail && hasPhone) {
            query = database.collection("users")
                    .where(Filter.or(
                            Filter.equalTo("email", user.getEmail()),
                            Filter.equalTo("phone", user.getPhone())
                    ));
        } else if (hasEmail) {
            query = database.collection("users")
                    .whereEqualTo("email", user.getEmail());
        } else {
            query = database.collection("users")
                    .whereEqualTo("phone", user.getPhone());
        }

        query.get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.isEmpty()) {
                        boolean emailExists = false;
                        boolean phoneExists = false;

                        for (var doc : snapshot.getDocuments()) {
                            User existing = doc.toObject(User.class);
                            if (existing == null) continue;

                            if (hasEmail && existing.getEmail() != null
                                    && existing.getEmail().equalsIgnoreCase(user.getEmail())) {
                                emailExists = true;
                            }

                            if (hasPhone && existing.getPhone() != null
                                    && existing.getPhone().equals(user.getPhone())) {
                                phoneExists = true;
                            }
                        }

                        if (emailExists) {
                            callback.onComplete(false, "Email already exists");
                            return;
                        }

                        if (phoneExists) {
                            callback.onComplete(false, "Phone already exists");
                            return;
                        }

                        callback.onComplete(false, "User already exists");
                        return;
                    }

                    saveUserToFirestore(user, callback);
                })
                .addOnFailureListener(e ->
                        callback.onComplete(false, e.getMessage()));
    }

    private void saveUserToFirestore(User user, RepositoryCallback<Boolean> callback) {
        database.collection("users")
                .document(user.getId())
                .set(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onComplete(true, null);
                    } else {
                        Exception e = task.getException();
                        callback.onComplete(false, e != null ? e.getMessage() : "Unknown Firestore error");
                    }
                });
    }

    public void authenticate(String emailOrPhone,
                             String password,
                             RepositoryCallback<User> callback) {

        if (emailOrPhone == null || emailOrPhone.isEmpty()
                || password == null || password.isEmpty()) {

            callback.onComplete(null, "Invalid credentials");
            return;
        }

        Query query = database.collection("users")
                .where(
                        Filter.or(
                                Filter.equalTo("email", emailOrPhone),
                                Filter.equalTo("phone", emailOrPhone)
                        )
                );

        query.get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {
                        callback.onComplete(null, "Invalid credentials");
                        return;
                    }

                    var document = snapshot.getDocuments().get(0);
                    User user = document.toObject(User.class);

                    if (user == null) {
                        callback.onComplete(null, "Invalid credentials");
                        return;
                    }

                    if (BCrypt.checkpw(password, user.getPassword())) {
                        callback.onComplete(user, null);
                    } else {
                        callback.onComplete(null, "Invalid credentials");
                    }

                })
                .addOnFailureListener(e ->
                        callback.onComplete(null, e.getMessage()));
    }

    public void getAllUsers(RepositoryCallback<List<User>> callback) {

        database.collection("users")
                .get()
                .addOnSuccessListener(snapshot -> {

                    List<User> users = new ArrayList<>();

                    snapshot.getDocuments().forEach(doc -> {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            users.add(user);
                        }
                    });

                    callback.onComplete(users, null);
                })
                .addOnFailureListener(e ->
                        callback.onComplete(null, e.getMessage()));
    }

    public void deleteUser(String userId, RepositoryCallback<Boolean> callback) {

        if (userId == null || userId.isEmpty()) {
            callback.onComplete(false, "UserId required");
            return;
        }

        database.collection("users")
                .document(userId)
                .delete()
                .addOnSuccessListener(unused -> callback.onComplete(true, null))
                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
    }

    public void findById(String userId, RepositoryCallback<User> callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onComplete(null, "User ID required");
            return;
        }

        database.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        callback.onComplete(null, "User not found");
                        return;
                    }

                    User user = snapshot.toObject(User.class);
                    if (user != null) {
                        user.setId(snapshot.getId());
                    }
                    callback.onComplete(user, null);
                })
                .addOnFailureListener(e ->
                        callback.onComplete(null, e.getMessage()));
    }
}