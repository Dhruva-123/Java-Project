package com.smartbank.model;

public class Admin extends User {
    private String role;

    public Admin(String adminId, String name, String passwordHash, String role) {
        super(adminId, name, passwordHash);
        this.role = role;
    }

    public String getAdminId() {
        return getUserId();
    }

    public void setAdminId(String adminId) {
        setUserId(adminId);
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
