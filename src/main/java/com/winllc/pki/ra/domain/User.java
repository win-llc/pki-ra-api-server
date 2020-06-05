package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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

    public User() {
    }

    public User(User user) {
        this(
                user.getIdentifier(),
                user.getUsername(),
                user.getRoles());
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