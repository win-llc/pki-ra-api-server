package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Account extends AbstractPersistable<Long> {
    private String keyIdentifier;
    private String macKey;
    private String projectName;
    private boolean acmeRequireHttpValidation = false;

    @JsonIgnore
    @OneToMany(mappedBy = "account")
    private Set<PocEntry> pocs;
    @JsonIgnore
    @ManyToMany
    private Set<User> accountUsers;
    @JsonIgnore
    @ManyToMany
    private Set<Domain> canIssueDomains;
    @ElementCollection
    @JsonIgnore
    private Set<String> preAuthorizationIdentifiers;

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

    public boolean isAcmeRequireHttpValidation() {
        return acmeRequireHttpValidation;
    }

    public void setAcmeRequireHttpValidation(boolean acmeRequireHttpValidation) {
        this.acmeRequireHttpValidation = acmeRequireHttpValidation;
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

    public Set<String> getPreAuthorizationIdentifiers() {
        return preAuthorizationIdentifiers;
    }

    public void setPreAuthorizationIdentifiers(Set<String> preAuthorizationIdentifiers) {
        this.preAuthorizationIdentifiers = preAuthorizationIdentifiers;
    }

    public void addPoc(PocEntry pocEntry){
        getPocs().add(pocEntry);
    }
}
