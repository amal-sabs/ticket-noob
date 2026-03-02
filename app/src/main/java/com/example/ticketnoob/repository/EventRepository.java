package com.example.ticketnoob.repository;

import com.example.ticketnoob.model.Event;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
public class EventRepository {
    private final FirebaseFirestore db;

    public EventRepository(){
        this.db = FirebaseFirestore.getInstance();
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
                            events.add(event);
                        }
                    });

                    callback.onComplete(events, null);
                })
                .addOnFailureListener( e ->
                        callback.onComplete(null, e.getMessage()));
    }
}
