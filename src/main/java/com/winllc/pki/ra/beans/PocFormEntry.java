package com.winllc.pki.ra.beans;

import java.util.Objects;

public class PocFormEntry {
    private Long id;
    private String email;
    private boolean owner;
    private boolean canManageAllServers;

    public PocFormEntry(String email) {
        this.email = email;
    }

    public PocFormEntry() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
    }

    public boolean isCanManageAllServers() {
        return canManageAllServers;
    }

    public void setCanManageAllServers(boolean canManageAllServers) {
        this.canManageAllServers = canManageAllServers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PocFormEntry that = (PocFormEntry) o;
        return Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }
}
