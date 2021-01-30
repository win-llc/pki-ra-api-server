package com.winllc.pki.ra.beans.info;

import com.winllc.pki.ra.domain.PocEntry;

import java.util.List;

public class PocEntryInfo {

    private String userId;
    private String fullName;
    private List<String> roles;
    private String addedOn;

    public PocEntryInfo(){}

    public PocEntryInfo(PocEntry pocEntry){
        this.userId = pocEntry.getEmail();
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
