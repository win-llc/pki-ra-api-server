package com.winllc.pki.ra.beans.info;

import com.winllc.pki.ra.beans.info.AccountInfo;
import com.winllc.pki.ra.domain.ServerEntry;

import java.util.List;

public class ServerEntryInfo extends InfoObject<ServerEntry> {

    private String fqdn;
    private AccountInfo accountInfo;
    private List<String> alternateDnsValues;
    private String openidClientId;
    private String openidClientRedirectUrl;

    public ServerEntryInfo(ServerEntry serverEntry){
        super(serverEntry);
        this.fqdn = serverEntry.getFqdn();
        this.alternateDnsValues = serverEntry.getAlternateDnsValues();
        this.setAccountInfo(new AccountInfo(serverEntry.getAccount(), false));
        this.openidClientId = serverEntry.getOpenidClientId();
        this.openidClientRedirectUrl = serverEntry.getOpenidClientRedirectUrl();
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

    public String getOpenidClientRedirectUrl() {
        return openidClientRedirectUrl;
    }

    public void setOpenidClientRedirectUrl(String openidClientRedirectUrl) {
        this.openidClientRedirectUrl = openidClientRedirectUrl;
    }
}
