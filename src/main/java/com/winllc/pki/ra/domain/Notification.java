package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.aspectj.weaver.ast.Not;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "notification")
public class Notification extends AbstractPersistable<Long> {

    @Column(nullable = false)
    private String forUserNames;
    @Column(nullable = false)
    private Timestamp created;
    private Timestamp expiresOn;
    private Boolean notificationRead = false;
    private String message;

    public static Notification buildNew(){
        Notification notification = new Notification();
        notification.setCreated(Timestamp.valueOf(LocalDateTime.now()));
        return notification;
    }

    public static Notification buildNew(String forUserName){
        Notification notification = buildNew();
        notification.setForUserNames(forUserName);
        notification.setCreated(Timestamp.valueOf(LocalDateTime.now()));
        return notification;
    }

    public static Notification buildNew(List<String> forUserNames){
        String combined = String.join(",", forUserNames);
        return buildNew(combined);
    }

    public Notification addMessage(String message){
        this.message = message;
        return this;
    }

    @JsonIgnore
    public List<String> getUserNamesAsList(){
        if(forUserNames != null){
            return Stream.of(forUserNames.split(",")).collect(Collectors.toList());
        }else{
            return new ArrayList<>();
        }
    }

    public String getForUserNames() {
        return forUserNames;
    }

    public void setForUserNames(String forUserNames) {
        this.forUserNames = forUserNames;
    }

    public Timestamp getCreated() {
        return created;
    }

    public void setCreated(Timestamp created) {
        this.created = created;
    }

    public Timestamp getExpiresOn() {
        return expiresOn;
    }

    public void setExpiresOn(Timestamp expiresOn) {
        this.expiresOn = expiresOn;
    }

    public Boolean getNotificationRead() {
        return notificationRead;
    }

    public void setNotificationRead(Boolean notificationRead) {
        this.notificationRead = notificationRead;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "forUserNames='" + forUserNames + '\'' +
                ", created=" + created +
                ", expiresOn=" + expiresOn +
                ", notificationRead=" + notificationRead +
                ", message='" + message + '\'' +
                "} " + super.toString();
    }
}
