package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.util.CollectionUtils;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.*;

@Entity
public class User extends AbstractPersistable<Long> {

    @NotNull
    private UUID identifier;
    @NotNull
    @Size(min = 1, max = 100)
    private String username;
    @ElementCollection
    @JsonIgnore
    private List<String> roles = new ArrayList<>();
    @JsonIgnore
    @OneToMany(mappedBy = "accountOwner")
    private Set<AccountRequest> accountRequests;
    @JsonIgnore
    @ManyToMany(mappedBy = "accountUsers")
    private Set<Account> accounts;
    @JsonIgnore
    @OneToMany(mappedBy = "addedByUser", cascade = CascadeType.PERSIST)
    private Set<AccountRestriction> accountRestrictionsAddedByUser;
    @JsonIgnore
    @OneToMany(mappedBy = "markedCompletedByUser", cascade = CascadeType.PERSIST)
    private Set<AccountRestriction> accountRestrictionsMarkedCompletedByUser;


    public User() {
    }

    public User(User user) {
        this(
                user.getIdentifier(),
                user.getUsername(),
                user.getRoles());
    }

    @PreRemove
    private void preRemove(){
        Set<Account> accounts = getAccounts();
        if(!CollectionUtils.isEmpty(accounts)) {
            for (Account account : accounts) {
                account.getAccountUsers().remove(this);
            }
        }
        Set<AccountRequest> requests = getAccountRequests();
        if(!CollectionUtils.isEmpty(requests)) {
            for (AccountRequest request : requests) {
                request.setAccountOwner(null);
            }
        }

        Set<AccountRestriction> markedCompletedByUser = getAccountRestrictionsMarkedCompletedByUser();
        if(!CollectionUtils.isEmpty(markedCompletedByUser)){
            for(AccountRestriction accountRestriction : markedCompletedByUser){
                accountRestriction.setMarkedCompletedByUser(null);
            }
        }

        Set<AccountRestriction> addedByUser = getAccountRestrictionsAddedByUser();
        if(!CollectionUtils.isEmpty(addedByUser)){
            for(AccountRestriction accountRestriction : addedByUser){
                accountRestriction.setAddedByUser(null);
            }
        }
    }

    @PersistenceConstructor
    public User(
            UUID identifier, String username, List<String> roles) {
        this.identifier = identifier;
        this.username = username;
        this.roles = roles;
    }

    public UUID getIdentifier() {
        return identifier;
    }

    public void setIdentifier(UUID identifier) {
        this.identifier = identifier;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Set<AccountRequest> getAccountRequests() {
        if(accountRequests == null) accountRequests = new HashSet<>();
        return accountRequests;
    }

    public void setAccountRequests(Set<AccountRequest> accountRequests) {
        this.accountRequests = accountRequests;
    }

    public Set<Account> getAccounts() {
        if(accounts == null) accounts = new HashSet<>();
        return accounts;
    }

    public void setAccounts(Set<Account> accounts) {
        this.accounts = accounts;
    }

    public Set<AccountRestriction> getAccountRestrictionsAddedByUser() {
        if(accountRestrictionsAddedByUser == null) accountRestrictionsAddedByUser = new HashSet<>();
        return accountRestrictionsAddedByUser;
    }

    public void setAccountRestrictionsAddedByUser(Set<AccountRestriction> accountRestrictionsAddedByUser) {
        this.accountRestrictionsAddedByUser = accountRestrictionsAddedByUser;
    }

    public Set<AccountRestriction> getAccountRestrictionsMarkedCompletedByUser() {
        if(accountRestrictionsMarkedCompletedByUser == null) accountRestrictionsMarkedCompletedByUser = new HashSet<>();
        return accountRestrictionsMarkedCompletedByUser;
    }

    public void setAccountRestrictionsMarkedCompletedByUser(Set<AccountRestriction> accountRestrictionsMarkedCompletedByUser) {
        this.accountRestrictionsMarkedCompletedByUser = accountRestrictionsMarkedCompletedByUser;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        User user = (User) o;
        return identifier.equals(user.identifier) &&
                username.equals(user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), identifier, roles);
    }
}