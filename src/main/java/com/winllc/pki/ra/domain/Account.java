package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Account extends BaseEntity {
    private String keyIdentifier;
    private String macKey;
    private String projectName;

    @OneToOne
    private User accountOwner;
    @JsonIgnore
    @OneToMany
    private Set<PocEntry> pocs;
    @JsonIgnore
    @ManyToMany
    private Set<User> accountUsers;
    @JsonIgnore
    @ManyToMany
    private Set<Domain> canIssueDomains;

    public String getKeyIdentifier() {
        return keyIdentifier;
    }

    public void setKeyIdentifier(String keyIdentifier) {
        this.keyIdentifier = keyIdentifier;
    }

    public String getMacKey() {
        return macKey;
    }

    public void setMacKey(String macKey) {
        this.macKey = macKey;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public User getAccountOwner() {
        return accountOwner;
    }

    public void setAccountOwner(User accountOwner) {
        this.accountOwner = accountOwner;
    }

    public Set<User> getAccountUsers() {
        if(accountUsers == null) accountUsers = new HashSet<>();
        return accountUsers;
    }

    public void setAccountUsers(Set<User> accountUsers) {
        this.accountUsers = accountUsers;
    }

    public Set<PocEntry> getPocs() {
        if(pocs == null) pocs = new HashSet<>();
        return pocs;
    }

    public void setPocs(Set<PocEntry> pocs) {
        this.pocs = pocs;
    }

    public Set<Domain> getCanIssueDomains() {
        if(canIssueDomains == null) canIssueDomains = new HashSet<>();
        return canIssueDomains;
    }

    public void setCanIssueDomains(Set<Domain> canIssueDomains) {
        this.canIssueDomains = canIssueDomains;
    }

    public void addPoc(PocEntry pocEntry){
        getPocs().add(pocEntry);
    }
}
