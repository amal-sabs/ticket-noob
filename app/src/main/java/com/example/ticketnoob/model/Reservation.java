package com.example.ticketnoob.model;

import java.io.Serializable;

public class Reservation implements Serializable{
    private String id;
    private String userId;
    private String eventId;
    private String status; // ACTIVE or CANCELLED
    private String timestamp;

    public Reservation(){}

    public Reservation(String id, String userId, String eventId, Status status, String timestamp){
        this.id = id;
        this.userId = userId;
        this.eventId = eventId;
        this.status = status != null ? status.name() : null;
        this.timestamp = timestamp;
    }

    public Reservation(String userId, String eventId, String timestamp){
        this.userId = userId;
        this.eventId = eventId;
        this.status = Status.ACTIVE.name();
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Status getStatusEnum(){
        return Status.fromString(status);
    }

    public void setStatusEnum(Status statusEnum){
        this.status = statusEnum != null ? statusEnum.name() : null;
    }
    public boolean isActive() {
        return Status.ACTIVE == getStatusEnum();
    }

    public boolean isCancelled() {
        return Status.CANCELLED == getStatusEnum();
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", eventId='" + eventId + '\'' +
                ", status='" + status + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
