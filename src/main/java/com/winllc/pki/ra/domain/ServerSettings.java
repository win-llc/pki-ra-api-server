package com.winllc.pki.ra.domain;

import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Entity;

@Entity
public class ServerSettings extends AbstractPersistable<Long> {

    private String emailServer;
    private Integer emailServerPort;
    private String emailFromAddress;

    private Boolean openIdConnectEnabled;
    private String openIdConnectServerBaseUrl;
    private String openIdConnectRealm;
    private String openIdConnectClientId;
    private String openIdConnectClientSecret;
    private String openIdConnectClientUserName;
    private String openIdConnectClientPassword;
    private String openIdConnectClientScope;

    public String getEmailServer() {
        return emailServer;
    }

    public void setEmailServer(String emailServer) {
        this.emailServer = emailServer;
    }

    public Integer getEmailServerPort() {
        return emailServerPort;
    }

    public void setEmailServerPort(Integer emailServerPort) {
        this.emailServerPort = emailServerPort;
    }

    public String getEmailFromAddress() {
        return emailFromAddress;
    }

    public void setEmailFromAddress(String emailFromAddress) {
        this.emailFromAddress = emailFromAddress;
    }

    public Boolean getOpenIdConnectEnabled() {
        return openIdConnectEnabled;
    }

    public void setOpenIdConnectEnabled(Boolean openIdConnectEnabled) {
        this.openIdConnectEnabled = openIdConnectEnabled;
    }

    public String getOpenIdConnectServerBaseUrl() {
        return openIdConnectServerBaseUrl;
    }

    public void setOpenIdConnectServerBaseUrl(String openIdConnectServerBaseUrl) {
        this.openIdConnectServerBaseUrl = openIdConnectServerBaseUrl;
    }

    public String getOpenIdConnectRealm() {
        return openIdConnectRealm;
    }

    public void setOpenIdConnectRealm(String openIdConnectRealm) {
        this.openIdConnectRealm = openIdConnectRealm;
    }

    public String getOpenIdConnectClientId() {
        return openIdConnectClientId;
    }

    public void setOpenIdConnectClientId(String openIdConnectClientId) {
        this.openIdConnectClientId = openIdConnectClientId;
    }

    public String getOpenIdConnectClientSecret() {
        return openIdConnectClientSecret;
    }

    public void setOpenIdConnectClientSecret(String openIdConnectClientSecret) {
        this.openIdConnectClientSecret = openIdConnectClientSecret;
    }

    public String getOpenIdConnectClientUserName() {
        return openIdConnectClientUserName;
    }

    public void setOpenIdConnectClientUserName(String openIdConnectClientUserName) {
        this.openIdConnectClientUserName = openIdConnectClientUserName;
    }

    public String getOpenIdConnectClientPassword() {
        return openIdConnectClientPassword;
    }

    public void setOpenIdConnectClientPassword(String openIdConnectClientPassword) {
        this.openIdConnectClientPassword = openIdConnectClientPassword;
    }

    public String getOpenIdConnectClientScope() {
        return openIdConnectClientScope;
    }

    public void setOpenIdConnectClientScope(String openIdConnectClientScope) {
        this.openIdConnectClientScope = openIdConnectClientScope;
    }
}
