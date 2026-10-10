package com.smartbank.model;

import com.smartbank.util.SecurityUtil;

public abstract class User {
    private String userId;
    private String name;
    private String passwordHash;

    public User(String userId, String name, String passwordHash) {
        this.userId = userId;
        this.name = name;
        this.passwordHash = passwordHash;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean login(String id, String password) {
        if (id == null || password == null) {
            return false;
        }
        if (!this.userId.equalsIgnoreCase(id.trim())) {
            return false;
        }
        String hashed = SecurityUtil.hash(password);
        return hashed != null && hashed.equalsIgnoreCase(this.passwordHash);
    }

    public void logout() {
    }

    public boolean changePassword(String oldPassword, String newPassword) {
        if (login(this.userId, oldPassword)) {
            this.passwordHash = SecurityUtil.hash(newPassword);
            return true;
        }
        return false;
    }
}
