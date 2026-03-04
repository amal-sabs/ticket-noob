package com.example.ticketnoob.model;

public enum EventStatus {
    ACTIVE,
    CANCELLED;

    public static EventStatus fromString(String value){
        if (value == null) return null;
        try{
            return EventStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e){
            return null;
        }
    }
}
