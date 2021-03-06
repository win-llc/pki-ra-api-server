package com.winllc.pki.ra.beans.info;

import com.winllc.acme.common.domain.PocEntry;

import java.util.Objects;

public class UserInfo extends InfoObject<PocEntry> {
    private String username;
    private boolean isOwner;

    public UserInfo(PocEntry pocEntry){
        super(pocEntry);
        this.username = pocEntry.getEmail();
        this.isOwner = pocEntry.isOwner();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isOwner() {
        return isOwner;
    }

    public void setOwner(boolean owner) {
        isOwner = owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserInfo userInfo = (UserInfo) o;
        return Objects.equals(username, userInfo.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
