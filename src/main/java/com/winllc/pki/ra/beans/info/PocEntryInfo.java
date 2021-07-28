package com.winllc.pki.ra.beans.info;

import com.winllc.acme.common.domain.PocEntry;

import java.util.List;

public class PocEntryInfo {

    private Long id;
    private String email;
    private String userId;
    private String fullName;
    private List<String> roles;
    private String addedOn;

    public PocEntryInfo(){}

    public PocEntryInfo(PocEntry pocEntry){
        this.id = pocEntry.getId();
        this.userId = pocEntry.getEmail();
        this.email = pocEntry.getEmail();
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getAddedOn() {
        return addedOn;
    }

    public void setAddedOn(String addedOn) {
        this.addedOn = addedOn;
    }
}
