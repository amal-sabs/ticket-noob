package com.example.ticketnoob.model;

public enum Status {
    ACTIVE,
    CANCELLED;

    public static Status fromString(String value){
        if (value == null) return null;
        try{
            return Status.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e){
            return null;
        }
    }
}
