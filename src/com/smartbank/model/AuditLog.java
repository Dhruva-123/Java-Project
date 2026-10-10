package com.smartbank.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuditLog {
    private String logId;
    private String adminId;
    private String action;
    private String targetId;
    private LocalDateTime dateTime;

    public AuditLog(String logId, String adminId, String action, String targetId, LocalDateTime dateTime) {
        this.logId = logId;
        this.adminId = adminId;
        this.action = action;
        this.targetId = targetId;
        this.dateTime = dateTime;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getDetails() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format("[%s] ID: %s | Admin: %s | Action: %s | Target: %s",
                dateTime != null ? dateTime.format(fmt) : "",
                logId, adminId, action, targetId);
    }

    public String toCsvLine() {
        return String.join(",",
                logId != null ? logId : "",
                adminId != null ? adminId : "",
                action != null ? action : "",
                targetId != null ? targetId : "",
                dateTime != null ? dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "");
    }

    public static AuditLog fromCsvLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 5) {
            return null;
        }
        return new AuditLog(
                parts[0].trim(),
                parts[1].trim(),
                parts[2].trim(),
                parts[3].trim(),
                LocalDateTime.parse(parts[4].trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }
}
