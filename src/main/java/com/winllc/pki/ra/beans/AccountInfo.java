package com.winllc.pki.ra.beans;

import com.nimbusds.jose.util.Base64;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.BaseEntity;

import java.util.List;
import java.util.Set;

public class AccountInfo extends InfoObject {

    private String keyIdentifier;
    private String macKey;
    private String macKeyBase64;
    private String projectName;
    private boolean acmeRequireHttpValidation;

    private UserInfo accountOwner;
    private List<UserInfo> pocs;
    private List<DomainInfo> canIssueDomains;

    public AccountInfo(Account entity) {
        super(entity);
        this.keyIdentifier = entity.getKeyIdentifier();
        this.macKey = entity.getMacKey();
        this.macKeyBase64 = Base64.encode(this.macKey).toString();
        this.projectName = entity.getProjectName();
        this.acmeRequireHttpValidation = entity.isAcmeRequireHttpValidation();
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

    public String getMacKeyBase64() {
        return macKeyBase64;
    }

    public void setMacKeyBase64(String macKeyBase64) {
        this.macKeyBase64 = macKeyBase64;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public UserInfo getAccountOwner() {
        return accountOwner;
    }

    public void setAccountOwner(UserInfo accountOwner) {
        this.accountOwner = accountOwner;
    }

    public List<UserInfo> getPocs() {
        return pocs;
    }

    public void setPocs(List<UserInfo> pocs) {
        this.pocs = pocs;
    }

    public List<DomainInfo> getCanIssueDomains() {
        return canIssueDomains;
    }

    public void setCanIssueDomains(List<DomainInfo> canIssueDomains) {
        this.canIssueDomains = canIssueDomains;
    }

    public boolean isAcmeRequireHttpValidation() {
        return acmeRequireHttpValidation;
    }

    public void setAcmeRequireHttpValidation(boolean acmeRequireHttpValidation) {
        this.acmeRequireHttpValidation = acmeRequireHttpValidation;
    }
}
