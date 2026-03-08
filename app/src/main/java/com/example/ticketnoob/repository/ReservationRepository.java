package com.example.ticketnoob.repository;

import com.example.ticketnoob.model.Reservation;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReservationRepository {
    private final FirebaseFirestore db;

    public ReservationRepository(){
        this.db = FirebaseFirestore.getInstance();
    }

    public void save(Reservation reservation, RepositoryCallback<Boolean> callback){
        if (reservation == null){
            callback.onComplete(false, "Reservation is null");
            return;
        }

        if (reservation.getId() == null || reservation.getId().isEmpty()){
            reservation.setId(UUID.randomUUID().toString());
        }

        db.collection("reservations")
                .document(reservation.getId())
                .set(reservation)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        callback.onComplete(true, null);
                    } else {
                        Exception e = task.getException();
                        callback.onComplete(false, e != null ? e.getMessage() : "Unknown Firestore error");
                    }
                });
    }

    public void findById(String reservationId, RepositoryCallback<Reservation> callback){
        if (reservationId == null || reservationId.isEmpty()){
            callback.onComplete(null, "Reservation ID required");
            return;
        }

        db.collection("reservations")
                .document(reservationId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()){
                        callback.onComplete(null, "Reservation not found");
                        return;
                    }

                    Reservation reservation = snapshot.toObject(Reservation.class);
                    if (reservation != null){
                        reservation.setId(snapshot.getId());
                    }
                    callback.onComplete(reservation, null);
                })
                .addOnFailureListener(e ->
                        callback.onComplete(null, e.getMessage()));
    }

    public void findActiveByUserId (String userId, RepositoryCallback<List<Reservation>> callback){
        if (userId == null || userId.isEmpty()){
            callback.onComplete(null, "User ID required");
            return;
        }

        db.collection("reservations")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Reservation> reservations = new ArrayList<>();

                    snapshot.getDocuments().forEach(doc -> {
                        Reservation reservation = doc.toObject(Reservation.class);
                        if (reservation != null){
                            reservation.setId(doc.getId());
                            reservations.add(reservation);
                        }
                    });

                    callback.onComplete(reservations, null);
                })
                .addOnFailureListener(e ->
                        callback.onComplete(null, e.getMessage()));
    }

    public void findAllByUserId (String userId, RepositoryCallback<List<Reservation>> callback){
        if (userId == null || userId.isEmpty()){
            callback.onComplete(null, "User ID required");
            return;
        }

        db.collection("reservations")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Reservation> reservations = new ArrayList<>();

                    snapshot.getDocuments().forEach(doc -> {
                        Reservation reservation = doc.toObject(Reservation.class);
                        if (reservation != null){
                            reservation.setId(doc.getId());
                            reservations.add(reservation);
                        }
                    });

                    callback.onComplete(reservations, null);
                })
                .addOnFailureListener(e ->
                        callback.onComplete(null, e.getMessage()));

    }

    public void update(Reservation reservation, RepositoryCallback<Boolean> callback){
        if (reservation == null){
            callback.onComplete(false, "Reservation is null");
            return;
        }

        if (reservation.getId() == null || reservation.getId().isEmpty()){
            callback.onComplete(false, "Reservation ID required");
            return;
        }

        db.collection("reservations")
                .document(reservation.getId())
                .set(reservation)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        callback.onComplete(true, null);
                    } else {
                        Exception e = task.getException();
                        callback.onComplete(false, e != null ? e.getMessage() : "Unknown Firestore error");
                    }
                });
    }

    public void findActiveByUserIdAndEventId(String userId, String eventId, RepositoryCallback<List<Reservation>> callback){
        if (userId == null || userId.isEmpty()){
            callback.onComplete(null, "User ID required");
            return;
        }

        if (eventId == null || eventId.isEmpty()){
            callback.onComplete(null, "Event ID required");
            return;
        }

        db.collection("reservations")
                .whereEqualTo("userId", userId)
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Reservation> reservations = new java.util.ArrayList<>();

                    snapshot.getDocuments().forEach(doc -> {
                        Reservation reservation = doc.toObject(Reservation.class);
                        if (reservation != null){
                            reservation.setId(doc.getId());
                            reservations.add(reservation);
                        }
                    });

                    callback.onComplete(reservations, null);
                })
                .addOnFailureListener(e ->
                        callback.onComplete(null, e.getMessage()));
    }

    public void findActiveByEventId(String eventId, RepositoryCallback<List<Reservation>> callback){
        if (eventId == null || eventId.isEmpty()){
            callback.onComplete(null, "Event ID required");
            return;
        }

        db.collection("reservations")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Reservation> reservations = new ArrayList<>();

                    snapshot.getDocuments().forEach(doc -> {
                        Reservation reservation = doc.toObject(Reservation.class);
                        if (reservation != null){
                            reservation.setId(doc.getId());
                            reservations.add(reservation);
                        }
                    });

                    callback.onComplete(reservations, null);
                })
                .addOnFailureListener(e ->
                        callback.onComplete(null, e.getMessage()));
    }
}
