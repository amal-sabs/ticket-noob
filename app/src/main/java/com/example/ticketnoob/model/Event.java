package com.example.ticketnoob.model;

import java.io.Serializable;
public class Event implements Serializable {
    private String id;
    private String title;
    private String description;
    private String date;
    private String location;
    private String category;
    private int capacity;
    private int availableSeats;
    private double price;
    private String status; // stored as string for firestore compatibility


    public Event(){}

    public Event(String id, String title, String description, String date, String location, String category, int capacity, int availableSeats, double price, EventStatus status){
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.location = location;
        this.category = category;
        this.capacity = capacity;
        this.availableSeats = availableSeats;
        this.price = price;
        this.status = status != null ? status.name() : null;
    }

    public Event(String title, String description, String date, String location, String category, int capacity, int availableSeats, double price, EventStatus status){
        this.title = title;
        this.description = description;
        this.date = date;
        this.location = location;
        this.category = category;
        this.capacity = capacity;
        this.availableSeats = availableSeats;
        this.price = price;
        this.status = status != null ? status.name() : null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public EventStatus getStatusEnum(){
        return EventStatus.fromString(status);
    }

    public void setStatusEnum(EventStatus statusEnum){
        this.status = statusEnum != null ? statusEnum.name() : null;
    }

    public boolean isActive() {
        return EventStatus.ACTIVE == getStatusEnum();
    }

    public boolean isCancelled() {
        return EventStatus.CANCELLED == getStatusEnum();
    }

    public boolean hasAvailableSeats() {
        return availableSeats > 0;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", date='" + date + '\'' +
                ", location='" + location + '\'' +
                ", category='" + category + '\'' +
                ", availableSeats=" + availableSeats +
                ", price=" + price +
                ", status='" + status + '\'' +
                '}';
    }
}
