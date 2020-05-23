package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.util.CollectionUtils;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
public class Account extends AbstractPersistable<Long> implements AccountOwnedEntity {
    @Column(unique = true)
    private String keyIdentifier;
    private String macKey;
    @Column(unique = true)
    private String projectName;
    private boolean acmeRequireHttpValidation = false;
    private boolean enabled = true;

    @JsonIgnore
    @OneToMany(mappedBy = "account", cascade = CascadeType.PERSIST)
    private Set<PocEntry> pocs;
    @JsonIgnore
    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE
    })
    @JoinTable(name = "account_user",
            joinColumns = @JoinColumn(name = "account_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> accountUsers;
    @JsonIgnore
    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE
    })
    @JoinTable(name = "account_domain",
            joinColumns = @JoinColumn(name = "account_id"),
            inverseJoinColumns = @JoinColumn(name = "domain_id")
    )
    private Set<Domain> canIssueDomains;
    @ElementCollection
    @JsonIgnore
    private Set<String> preAuthorizationIdentifiers;
    @JsonIgnore
    @OneToMany(mappedBy = "account", cascade = CascadeType.PERSIST)
    private Set<AccountRestriction> accountRestrictions;
    @JsonIgnore
    @OneToMany(mappedBy = "account", cascade = CascadeType.PERSIST)
    private Set<CertificateRequest> certificateRequests;
    @JsonIgnore
    @OneToMany(mappedBy = "account", cascade = CascadeType.PERSIST)
    private Set<ServerEntry> serverEntries;

    @PreRemove
    private void preRemove(){
        Set<CertificateRequest> requests = getCertificateRequests();
        if(!CollectionUtils.isEmpty(requests)){
            for(CertificateRequest request : certificateRequests){
                request.setAccount(null);
            }
        }

        Set<ServerEntry> serverEntries = getServerEntries();
        if(!CollectionUtils.isEmpty(serverEntries)){
            for(ServerEntry serverEntry : serverEntries){
                serverEntry.setAccount(null);
            }
        }

    }

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public Set<AccountRestriction> getAccountRestrictions() {
        return accountRestrictions;
    }

    public void setAccountRestrictions(Set<AccountRestriction> accountRestrictions) {
        this.accountRestrictions = accountRestrictions;
    }

    public void addPoc(PocEntry pocEntry){
        getPocs().add(pocEntry);
    }

    public Set<CertificateRequest> getCertificateRequests() {
        return certificateRequests;
    }

    public void setCertificateRequests(Set<CertificateRequest> certificateRequests) {
        this.certificateRequests = certificateRequests;
    }

    public Set<ServerEntry> getServerEntries() {
        if(serverEntries == null) serverEntries = new HashSet<>();
        return serverEntries;
    }

    public void setServerEntries(Set<ServerEntry> serverEntries) {
        this.serverEntries = serverEntries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Account account = (Account) o;
        return Objects.equals(keyIdentifier, account.keyIdentifier) &&
                Objects.equals(macKey, account.macKey) &&
                Objects.equals(projectName, account.projectName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), keyIdentifier, macKey, projectName);
    }

    @Override
    @JsonIgnore
    public Account getAccount() {
        return this;
    }
}
