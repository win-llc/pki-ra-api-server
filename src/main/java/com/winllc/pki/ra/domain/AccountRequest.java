package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.util.CollectionUtils;

import javax.persistence.*;
import java.util.Set;

@Entity
public class AccountRequest extends AbstractPersistable<Long> {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="accountOwner_fk")
    private User accountOwner;
    private String projectName;
    @Column(nullable = false)
    private String state;

    public static AccountRequest createNew(){
        AccountRequest request = new AccountRequest();
        request.setState("new");
        return request;
    }

    @PreRemove
    private void preRemove(){
        setAccountOwner(null);

        if(accountOwner != null){
            Set<AccountRequest> accountRequests = accountOwner.getAccountRequests();
            if(!CollectionUtils.isEmpty(accountRequests)){
                accountRequests.remove(this);
            }
        }
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
