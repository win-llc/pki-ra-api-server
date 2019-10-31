package com.winllc.pki.ra.beans;

import com.winllc.pki.ra.domain.User;
import org.springframework.data.jpa.domain.AbstractPersistable;

public class UserInfo extends InfoObject {
    private String username;
    private String email;

    public UserInfo(User entity) {
        super(entity);
        this.username = entity.getUsername();
        this.email = entity.getEmail();
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
}
