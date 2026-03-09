package com.example.ticketnoob.service;

import com.example.ticketnoob.repository.RepositoryCallback;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Writes an email request document to the "mail" collection in Firestore.
 * A Firebase Cloud Function (trigger on mail collection) picks it up
 * and sends the actual email via a provider.
 *
 * Requires the "Trigger Email from Firestore" Firebase Extension or
 * a custom Cloud Function listening on the "mail" collection.
 *
 * https://extensions.dev/extensions/firebase/firestore-send-email
 */
public class FirebaseEmailSender implements EmailSender {

    private final FirebaseFirestore database;

    public FirebaseEmailSender() {
        this.database = FirebaseFirestore.getInstance();
    }

    // Constructor for testing with injected Firestore instance
    public FirebaseEmailSender(FirebaseFirestore database) {
        this.database = database;
    }

    @Override
    public void send(String to, String subject, String body, RepositoryCallback<Boolean> callback) {
        if (to == null || to.isEmpty()) {
            callback.onComplete(false, "Recipient email required");
            return;
        }

        if (subject == null || subject.isEmpty()) {
            callback.onComplete(false, "Subject required");
            return;
        }

        if (body == null || body.isEmpty()) {
            callback.onComplete(false, "Body required");
            return;
        }

        // Structure expected by the Firebase "Trigger Email" extension
        Map<String, Object> message = new HashMap<>();
        message.put("subject", subject);
        message.put("text", body);

        Map<String, Object> mailDoc = new HashMap<>();
        mailDoc.put("to", to);
        mailDoc.put("message", message);

        database.collection("mail")
                .add(mailDoc)
                .addOnSuccessListener(docRef -> callback.onComplete(true, null))
                .addOnFailureListener(e -> callback.onComplete(false,
                        e.getMessage() != null ? e.getMessage() : "Failed to queue email"));
    }
}