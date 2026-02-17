package com.example.ticketnoob.model;

import java.io.Serializable;

public class User implements Serializable {

    // Fields
    private String id;          // Unique user ID
    private String name;        // Full name
    private String email;       // Email (optional if phone is used)
    private String phone;       // Phone number (optional if email is used)
    private String password;    // Password (hashed in production)
    private String role;        // "CUSTOMER" or "ADMIN"

    // Default constructor
    public User() {
    }

    // Full constructor
    public User(String id, String name, String email, String phone, String password, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
    }

    // Partial constructor for registration
    public User(String name, String email, String phone, String password, String role) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // Helper Methods

    // Check if user is admin
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    // Check if user is customer
    public boolean isCustomer() {
        return "CUSTOMER".equalsIgnoreCase(role);
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
