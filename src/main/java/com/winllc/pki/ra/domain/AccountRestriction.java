package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winllc.pki.ra.constants.AccountRestrictionAction;
import com.winllc.pki.ra.constants.AccountRestrictionType;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.sql.Timestamp;

@Entity
public class AccountRestriction extends AbstractPersistable<Long> implements AccountOwnedEntity {

    private AccountRestrictionType type;
    private AccountRestrictionAction action;
    private Timestamp createdOn;
    private Timestamp dueBy;
    private boolean completed = false;
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name="account_fk")
    private Account account;
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name="addedByUser_fk")
    private User addedByUser;
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name="markCompletedByUser_fk")
    private User markedCompletedByUser;

    public AccountRestrictionType getType() {
        return type;
    }

    public void setType(AccountRestrictionType type) {
        this.type = type;
    }

    public AccountRestrictionAction getAction() {
        return action;
    }

    public void setAction(AccountRestrictionAction action) {
        this.action = action;
    }

    public Timestamp getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Timestamp createdOn) {
        this.createdOn = createdOn;
    }

    public Timestamp getDueBy() {
        return dueBy;
    }

    public void setDueBy(Timestamp dueBy) {
        this.dueBy = dueBy;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public User getAddedByUser() {
        return addedByUser;
    }

    public void setAddedByUser(User addedByUser) {
        this.addedByUser = addedByUser;
    }

    public User getMarkedCompletedByUser() {
        return markedCompletedByUser;
    }

    public void setMarkedCompletedByUser(User markedCompletedByUser) {
        this.markedCompletedByUser = markedCompletedByUser;
    }
}
