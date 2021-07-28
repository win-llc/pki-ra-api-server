package com.winllc.pki.ra.beans.info;

import com.winllc.acme.common.domain.ServerEntry;
import org.hibernate.Hibernate;

import java.util.List;

public class ServerEntryInfo extends InfoObject<ServerEntry> {

    private String fqdn;
    private AccountInfo accountInfo;
    private List<String> alternateDnsValues;
    private String openidClientId;
    private String openidClientRedirectUrl;
    private String creationDate;

    public ServerEntryInfo(ServerEntry serverEntry){
        super(serverEntry);
        this.fqdn = serverEntry.getFqdn();
        Hibernate.initialize(serverEntry.getAlternateDnsValues());
        this.alternateDnsValues = serverEntry.getAlternateDnsValues();
        this.setAccountInfo(new AccountInfo(serverEntry.getAccount(), false));
        this.openidClientId = serverEntry.getOpenidClientId();
        this.openidClientRedirectUrl = serverEntry.getOpenidClientRedirectUrl();
        if(serverEntry.getCreationDate() != null){
            this.creationDate = serverEntry.getCreationDate().toString();
        }
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

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }
}
