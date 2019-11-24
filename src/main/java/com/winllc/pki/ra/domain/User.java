package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.jpa.domain.AbstractPersistable;

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
    @Size(min = 1, max = 100)
    @Email
    private String email;
    @ElementCollection
    @JsonIgnore
    private List<String> roles = new ArrayList<>();
    @JsonIgnore
    @OneToMany
    private Set<AccountRequest> accountRequests;
    @JsonIgnore
    @ManyToMany
    private Set<Account> accounts;

    public User() {
    }

    public User(User user) {
        this(
                user.getIdentifier(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles());
    }

    @PersistenceConstructor
    public User(
            UUID identifier, String username, String email, List<String> roles) {
        this.identifier = identifier;
        this.username = username;
        this.email = email;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Set<AccountRequest> getAccountRequests() {
        return accountRequests;
    }

    public void setAccountRequests(Set<AccountRequest> accountRequests) {
        this.accountRequests = accountRequests;
    }

    public Set<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(Set<Account> accounts) {
        this.accounts = accounts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        User user = (User) o;
        return identifier.equals(user.identifier)
                && email.equals(user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), identifier, email, roles);
    }
}