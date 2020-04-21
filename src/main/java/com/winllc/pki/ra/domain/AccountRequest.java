package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

@Entity
public class AccountRequest extends AbstractPersistable<Long> {

    @JsonIgnore
    @ManyToOne
    private User accountOwner;
    private String projectName;
    private String state;

    public static AccountRequest createNew(){
        AccountRequest request = new AccountRequest();
        request.setState("new");
        return request;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public User getAccountOwner() {
        return accountOwner;
    }

    public void setAccountOwner(User accountOwner) {
        this.accountOwner = accountOwner;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void approve(){
        setState("approve");
    }

    public void reject(){
        setState("reject");
    }
}
