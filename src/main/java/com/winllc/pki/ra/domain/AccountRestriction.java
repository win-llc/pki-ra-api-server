package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winllc.pki.ra.constants.AccountRestrictionAction;
import com.winllc.pki.ra.constants.AccountRestrictionType;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "account_restriction")
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
    private String addedByUser;
    private String markedCompletedByUser;

    @PreRemove
    private void preRemove(){
        if(account != null){
            account.getAccountRestrictions().remove(this);
        }
    }

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

    public String getAddedByUser() {
        return addedByUser;
    }

    public void setAddedByUser(String addedByUser) {
        this.addedByUser = addedByUser;
    }

    public String getMarkedCompletedByUser() {
        return markedCompletedByUser;
    }

    public void setMarkedCompletedByUser(String markedCompletedByUser) {
        this.markedCompletedByUser = markedCompletedByUser;
    }
}
