package com.winllc.pki.ra.domain;

import com.winllc.acme.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.lang.reflect.Constructor;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class RequestEntity extends BaseEntity {

    @Column(nullable = false)
    protected String status;
    protected Timestamp requestedOn;
    protected String requestedBy;
    protected Timestamp statusUpdatedOn;
    protected String decisionMadeBy;

    public RequestEntity(String requestedBy) {
        this.requestedBy = requestedBy;
        this.requestedOn = Timestamp.valueOf(LocalDateTime.now());
        this.status = "new";
    }

    public RequestEntity() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getRequestedOn() {
        return requestedOn;
    }

    public void setRequestedOn(Timestamp requestedOn) {
        this.requestedOn = requestedOn;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Timestamp getStatusUpdatedOn() {
        return statusUpdatedOn;
    }

    public void setStatusUpdatedOn(Timestamp statusUpdatedOn) {
        this.statusUpdatedOn = statusUpdatedOn;
    }

    public String getDecisionMadeBy() {
        return decisionMadeBy;
    }

    public void setDecisionMadeBy(String decisionMadeBy) {
        this.decisionMadeBy = decisionMadeBy;
    }
}
