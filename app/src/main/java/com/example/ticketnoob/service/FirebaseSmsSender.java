package com.example.ticketnoob.service;

import com.example.ticketnoob.repository.RepositoryCallback;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Writes an SMS request document to the "sms" collection in Firestore.
 * A Firebase Cloud Function (trigger on sms collection) picks it up
 * and sends the actual SMS via a provider.
 *
 * Requires a custom Cloud Function listening on the "sms" collection.
 * Example: https://firebase.google.com/docs/functions/firestore-events
 */
public class FirebaseSmsSender implements SmsSender {

    private final FirebaseFirestore database;

    public FirebaseSmsSender() {
        this.database = FirebaseFirestore.getInstance();
    }

    // Constructor for testing with injected Firestore instance
    public FirebaseSmsSender(FirebaseFirestore database) {
        this.database = database;
    }

    @Override
    public void send(String to, String message, RepositoryCallback<Boolean> callback) {
        if (to == null || to.isEmpty()) {
            callback.onComplete(false, "Recipient phone number required");
            return;
        }

        if (message == null || message.isEmpty()) {
            callback.onComplete(false, "Message required");
            return;
        }

        Map<String, Object> smsDoc = new HashMap<>();
        smsDoc.put("to", to);
        smsDoc.put("message", message);

        database.collection("sms")
                .add(smsDoc)
                .addOnSuccessListener(docRef -> callback.onComplete(true, null))
                .addOnFailureListener(e -> callback.onComplete(false,
                        e.getMessage() != null ? e.getMessage() : "Failed to queue SMS"));
    }
}