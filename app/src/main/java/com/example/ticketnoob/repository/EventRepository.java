package com.example.ticketnoob.repository;

import com.example.ticketnoob.model.Event;
import com.example.ticketnoob.model.Status;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import java.util.HashMap;
import java.util.Map;
public class EventRepository {
    private final FirebaseFirestore db;

    public EventRepository(){
        this.db = FirebaseFirestore.getInstance();
    }

    // This is just to populate the db with some events
    public void seedSampleEvents() {
        addEvent("Jazz Night", "2026-03-10 19:30", "Montreal, QC", "Music", 35.00, 120, Status.ACTIVE.name());
        addEvent("Tech Conference", "2026-04-01 09:00", "Laval, QC", "Conference", 199.99, 45, Status.ACTIVE.name());
        addEvent("Food Festival", "2026-03-20 12:00", "Downtown", "Festival", 12.50, 300, Status.ACTIVE.name());
    }

    private void addEvent(String title, String date, String location, String category, double price, int capacity, String status) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("description", title);
        data.put("date", date);
        data.put("location", location);
        data.put("category", category);
        data.put("price", price);
        data.put("availableSeats", capacity);
        data.put("capacity", capacity);
        data.put("status", status);

        // Auto-ID document
        db.collection("events").add(data);
    }

    public void save(Event event, RepositoryCallback<Boolean> callback){
        if (event == null){
            callback.onComplete(false, "Event is null");
            return;
        }

        if (event.getId() == null || event.getId().isEmpty()){
            event.setId(UUID.randomUUID().toString());
        }

        db.collection("events")
                .document(event.getId())
                .set(event)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        callback.onComplete(true, null);
                    } else{
                        Exception e = task.getException();
                        callback.onComplete(false, e != null ? e.getMessage() : "Unknown Firestore error");
                    }
                });
    }

    public void findById(String eventId, RepositoryCallback<Event> callback){
        if(eventId == null || eventId.isEmpty()){
            callback.onComplete(null, "Event ID required");
            return;
        }

        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()){
                        callback.onComplete(null, "Event not found");
                        return;
                    }

                    Event event = snapshot.toObject(Event.class);
                    if (event != null){
                        event.setId(snapshot.getId());
                    }
                    callback.onComplete(event, null);
                })
                .addOnFailureListener(e ->
                        callback.onComplete(null, e.getMessage()));
    }

    public void getAllActiveEvents(RepositoryCallback<List<Event>> callback){
        db.collection("events")
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Event> events = new ArrayList<>();

                    snapshot.getDocuments().forEach(doc -> {
                        Event event = doc.toObject(Event.class);
                        if (event != null){
                            event.setId(doc.getId());
                            events.add(event);
                        }
                    });
                    callback.onComplete(events, null);
                })
                .addOnFailureListener(e ->
                        callback.onComplete(null, e.getMessage()));
    }

    public void getAllEvents(RepositoryCallback<List<Event>> callback){
        db.collection("events")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Event> events = new ArrayList<>();

                    snapshot.getDocuments().forEach(doc -> {
                        Event event = doc.toObject(Event.class);
                        if (event != null){
                            event.setId(doc.getId());
                            events.add(event);
                        }
                    });

                    callback.onComplete(events, null);
                })
                .addOnFailureListener( e ->
                        callback.onComplete(null, e.getMessage()));
    }

    public void update(Event event, RepositoryCallback<Boolean> callback){
        if (event == null){
            callback.onComplete(false, "Event is null");
            return;
        }

        if (event.getId() == null || event.getId().isEmpty()){
            callback.onComplete(false, "Event ID required");
            return;
        }

        db.collection("events")
                .document(event.getId())
                .set(event)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        callback.onComplete(true, null);
                    } else{
                        Exception e = task.getException();
                        callback.onComplete(false, e != null ? e.getMessage() : "Unknown Firestore error");
                    }
                });
    }
}
