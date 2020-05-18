package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
public class ServerEntry extends AbstractPersistable<Long> implements AccountOwnedEntity {

    private String hostname;
    private String fqdn;
    @JsonIgnore
    @ElementCollection
    private List<String> alternateDnsValues;
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name="domainParent_fk")
    private Domain domainParent;
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name="account_fk")
    private Account account;
    @JsonIgnore
    @ManyToMany(mappedBy = "serverEntries")
    private Set<AttributePolicyGroup> policyGroups;
    @JsonIgnore
    @OneToMany(mappedBy = "serverEntry")
    private Set<CertificateRequest> certificateRequests;
    private String openidClientId;
    private String openidClientRedirectUrl;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getFqdn() {
        return fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public Domain getDomainParent() {
        return domainParent;
    }

    public void setDomainParent(Domain domainParent) {
        this.domainParent = domainParent;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getOpenidClientId() {
        return openidClientId;
    }

    public void setOpenidClientId(String openidClientId) {
        this.openidClientId = openidClientId;
    }

    public List<String> getAlternateDnsValues() {
        return alternateDnsValues;
    }

    public void setAlternateDnsValues(List<String> alternateDnsValues) {
        this.alternateDnsValues = alternateDnsValues;
    }

    public String getOpenidClientRedirectUrl() {
        return openidClientRedirectUrl;
    }

    public void setOpenidClientRedirectUrl(String openidClientRedirectUrl) {
        this.openidClientRedirectUrl = openidClientRedirectUrl;
    }

    public Set<AttributePolicyGroup> getPolicyGroups() {
        if(policyGroups == null) policyGroups = new HashSet<>();
        return policyGroups;
    }

    public void setPolicyGroups(Set<AttributePolicyGroup> policyGroups) {
        this.policyGroups = policyGroups;
    }

    public Set<CertificateRequest> getCertificateRequests() {
        if(certificateRequests == null) certificateRequests = new HashSet<>();
        return certificateRequests;
    }

    public void setCertificateRequests(Set<CertificateRequest> certificateRequests) {
        this.certificateRequests = certificateRequests;
    }

    @Override
    public String toString() {
        return "ServerEntry{" +
                "hostname='" + hostname + '\'' +
                ", fqdn='" + fqdn + '\'' +
                ", openidClientId='" + openidClientId + '\'' +
                "} " + super.toString();
    }
}
