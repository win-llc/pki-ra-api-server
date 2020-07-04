package com.winllc.pki.ra.beans.info;

import com.winllc.pki.ra.domain.PocEntry;

import java.util.Objects;

public class UserInfo extends InfoObject<PocEntry> {
    private String username;

    public UserInfo(PocEntry pocEntry){
        super(pocEntry);
        this.username = pocEntry.getEmail();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
