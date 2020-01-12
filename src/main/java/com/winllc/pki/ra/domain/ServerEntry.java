package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
public class ServerEntry extends AbstractPersistable<Long> {

    private String hostname;
    private String fqdn;
    @JsonIgnore
    @ManyToOne
    private Domain domainParent;
    @JsonIgnore
    @ManyToOne
    private Account account;
    private String openidClientId;

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

    @Override
    public String toString() {
        return "ServerEntry{" +
                "hostname='" + hostname + '\'' +
                ", fqdn='" + fqdn + '\'' +
                ", openidClientId='" + openidClientId + '\'' +
                "} " + super.toString();
    }
}