package com.winllc.pki.ra.beans;

import com.winllc.pki.ra.domain.ServerEntry;

import java.util.List;

public class ServerEntryInfo {

    private Long id;
    private String fqdn;
    private AccountInfo accountInfo;
    private List<String> alternateDnsValues;
    private String openidClientId;

    public ServerEntryInfo(ServerEntry serverEntry){
        this.id = serverEntry.getId();
        this.fqdn = serverEntry.getFqdn();
        this.alternateDnsValues = serverEntry.getAlternateDnsValues();
        this.setAccountInfo(new AccountInfo(serverEntry.getAccount()));
        this.openidClientId = serverEntry.getOpenidClientId();
    }

    private ServerEntryInfo(){}

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

    public AccountInfo getAccountInfo() {
        return accountInfo;
    }

    public void setAccountInfo(AccountInfo accountInfo) {
        this.accountInfo = accountInfo;
    }

    public List<String> getAlternateDnsValues() {
        return alternateDnsValues;
    }

    public void setAlternateDnsValues(List<String> alternateDnsValues) {
        this.alternateDnsValues = alternateDnsValues;
    }

    public String getOpenidClientId() {
        return openidClientId;
    }

    public void setOpenidClientId(String openidClientId) {
        this.openidClientId = openidClientId;
    }
}
