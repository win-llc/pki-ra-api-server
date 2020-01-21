package com.winllc.pki.ra.beans;

import com.winllc.pki.ra.domain.ServerEntry;

import java.util.List;

public class ServerEntryForm {

    private Long id;
    private String fqdn;
    private Long accountId;
    private List<String> alternateDnsValues;
    private String openidClientRedirectUrl;

    public ServerEntryForm(){}

    public ServerEntryForm(ServerEntry entry){
        this.id = entry.getId();
        this.fqdn = entry.getFqdn();
        this.accountId = entry.getAccount().getId();
        this.alternateDnsValues = entry.getAlternateDnsValues();
        this.openidClientRedirectUrl = entry.getOpenidClientRedirectUrl();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFqdn() {
        return fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
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
}
