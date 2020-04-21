package com.winllc.pki.ra.beans;

import java.util.Objects;

public class PocFormEntry {
    private String id;
    private String email;

    public PocFormEntry(String email) {
        this.email = email;
    }

    public PocFormEntry() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
