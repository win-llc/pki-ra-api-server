package com.winllc.pki.ra.beans.info;

import com.winllc.pki.ra.beans.PocFormEntry;
import com.winllc.acme.common.contants.DateTimeUtil;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.AuthCredential;

import java.util.List;
import java.util.Optional;

public class AccountInfo extends InfoObject<Account> {

    private String keyIdentifier;
    private String macKey;
    private String macKeyBase64;
    private String projectName;
    private String entityBaseDn;
    private boolean acmeRequireHttpValidation;
    private boolean enabled;
    private String securityPolicyServerProjectId;
    private String creationDate;

    private UserInfo accountOwner;
    private boolean userIsOwner = false;
    private List<PocFormEntry> pocs;
    private List<DomainInfo> canIssueDomains;

    public AccountInfo(Account entity, boolean loadKeys) {
        super(entity);
        if(loadKeys) {
            Optional<AuthCredential> optionalCredential = entity.getLatestAuthCredential();
            if(optionalCredential.isPresent()){
                AuthCredential authCredential = optionalCredential.get();
                this.keyIdentifier = authCredential.getKeyIdentifier();
                this.macKey = authCredential.getMacKey();
                this.macKeyBase64 = authCredential.getMacKeyBase64();
            }
        }
        this.projectName = entity.getProjectName();
        this.entityBaseDn = entity.getEntityBaseDn();
        this.enabled = entity.isEnabled();
        this.securityPolicyServerProjectId = entity.getSecurityPolicyServerProjectId();
        if(entity.getCreationDate() != null) {
            this.creationDate = DateTimeUtil.DATE_TIME_FORMATTER.format(entity.getCreationDate().toInstant());
        }
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

    public List<PocFormEntry> getPocs() {
        return pocs;
    }

    public void setPocs(List<PocFormEntry> pocs) {
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

    public boolean isUserIsOwner() {
        return userIsOwner;
    }

    public void setUserIsOwner(boolean userIsOwner) {
        this.userIsOwner = userIsOwner;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }


}
