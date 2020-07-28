package com.winllc.pki.ra.beans.info;

import com.nimbusds.jose.util.Base64;
import com.winllc.pki.ra.domain.Account;

import java.util.ArrayList;
import java.util.List;

public class AccountInfo extends InfoObject<Account> {

    private String keyIdentifier;
    private String macKey;
    private String macKeyBase64;
    private String projectName;
    private String entityBaseDn;
    private boolean acmeRequireHttpValidation;
    private boolean enabled;
    private String securityPolicyServerProjectId;

    private UserInfo accountOwner;
    private List<UserInfo> pocs;
    private List<DomainInfo> canIssueDomains;

    private List<AcmeConnectionInfo> acmeConnectionInfoList;

    public AccountInfo(Account entity, boolean loadKeys) {
        super(entity);
        if(loadKeys) {
            this.keyIdentifier = entity.getKeyIdentifier();
            this.macKey = entity.getMacKey();
            this.macKeyBase64 = entity.getMacKeyBase64();
        }
        this.projectName = entity.getProjectName();
        this.entityBaseDn = entity.getEntityBaseDn();
        this.enabled = entity.isEnabled();
        this.securityPolicyServerProjectId = entity.getSecurityPolicyServerProjectId();
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

    public String getEntityBaseDn() {
        return entityBaseDn;
    }

    public void setEntityBaseDn(String entityBaseDn) {
        this.entityBaseDn = entityBaseDn;
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

    public List<AcmeConnectionInfo> getAcmeConnectionInfoList() {
        if(acmeConnectionInfoList == null) acmeConnectionInfoList = new ArrayList<>();
        return acmeConnectionInfoList;
    }

    public void setAcmeConnectionInfoList(List<AcmeConnectionInfo> acmeConnectionInfoList) {
        this.acmeConnectionInfoList = acmeConnectionInfoList;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSecurityPolicyServerProjectId() {
        return securityPolicyServerProjectId;
    }

    public void setSecurityPolicyServerProjectId(String securityPolicyServerProjectId) {
        this.securityPolicyServerProjectId = securityPolicyServerProjectId;
    }

    public static class AcmeConnectionInfo {
        private String directory;

        private String url;
        private String macKeyBase64;
        private String accountKeyId;

        public String getDirectory() {
            return directory;
        }

        public void setDirectory(String directory) {
            this.directory = directory;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getMacKeyBase64() {
            return macKeyBase64;
        }

        public void setMacKeyBase64(String macKeyBase64) {
            this.macKeyBase64 = macKeyBase64;
        }

        public String getAccountKeyId() {
            return accountKeyId;
        }

        public void setAccountKeyId(String accountKeyId) {
            this.accountKeyId = accountKeyId;
        }
    }
}
