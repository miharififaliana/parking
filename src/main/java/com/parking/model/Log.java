package com.parking.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entrée du journal d'audit (table {@code logs}).
 */
public class Log {

    private int idLog;
    private int idUser;
    private String action;
    private String detail;
    private String ipAddress;
    private LocalDateTime createdAt;

    public Log() {
    }

    public Log(int idUser, String action, String detail) {
        this.idUser = idUser;
        this.action = action;
        this.detail = detail;
    }

    public int getIdLog() {
        return idLog;
    }

    public void setIdLog(int idLog) {
        this.idLog = idLog;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Log log)) return false;
        return idLog == log.idLog;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idLog);
    }

    @Override
    public String toString() {
        return "Log{id=" + idLog + ", action='" + action + "', user=" + idUser + "}";
    }
}
