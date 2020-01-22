package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.util.List;

@Entity
public class ServerEntry extends AbstractPersistable<Long> {

    private String hostname;
    @Column(unique = true)
    private String fqdn;
    @JsonIgnore
    @ElementCollection
    private List<String> alternateDnsValues;
    @JsonIgnore
    @ManyToOne
    private Domain domainParent;
    @JsonIgnore
    @ManyToOne
    private Account account;
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

    @Override
    public String toString() {
        return "ServerEntry{" +
                "hostname='" + hostname + '\'' +
                ", fqdn='" + fqdn + '\'' +
                ", openidClientId='" + openidClientId + '\'' +
                "} " + super.toString();
    }
}
