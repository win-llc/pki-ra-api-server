package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nimbusds.jose.util.Base64;
import com.winllc.pki.ra.util.AppUtil;
import org.springframework.ldap.odm.annotations.Transient;
import org.springframework.util.CollectionUtils;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "account")
public class Account extends AuthCredentialHolder implements AccountOwnedEntity, DomainCertIssuanceRestrictionHolder {
    @Column(unique = true)
    private String keyIdentifier;
    //private String macKey;
    @Column(unique = true)
    private String projectName;
    private String entityBaseDn;
    private boolean enabled = true;
    private String securityPolicyServerProjectId;
    private boolean allowHostnameIssuance = true;

    @JsonIgnore
    @OneToMany(mappedBy = "parentEntity", fetch = FetchType.EAGER)
    @Transient
    private Set<AuthCredential> authCredentials;

    @JsonIgnore
    @OneToMany(mappedBy = "account", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private Set<PocEntry> pocs;
    @JsonIgnore
    @OneToMany(mappedBy = "account", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private Set<AccountRestriction> accountRestrictions;
    @JsonIgnore
    @OneToMany(mappedBy = "account", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private Set<CertificateRequest> certificateRequests;
    @JsonIgnore
    @OneToMany(mappedBy = "account", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private Set<ServerEntry> serverEntries;
    @JsonIgnore
    @OneToMany(mappedBy = "account", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private Set<IssuedCertificate> issuedCertificates;
    @JsonIgnore
    @OneToMany(mappedBy = "account", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private Set<AttributePolicyGroup> policyGroups;

    @JsonIgnore
    @OneToMany(mappedBy = "account", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private Set<DomainPolicy> accountDomainPolicies;

    public static Account buildNew(String projectName){
        Account account = new Account();
        account.setUuid(UUID.randomUUID().toString());

        //String macKey = AppUtil.generate256BitString();
        String keyIdentifier = AppUtil.generate20BitString();

        account.setKeyIdentifier(keyIdentifier);
        //account.setMacKey(macKey);
        account.setProjectName(projectName);

        return account;
    }

    @PreRemove
    private void preRemove(){
        Set<CertificateRequest> requests = getCertificateRequests();
        if(!CollectionUtils.isEmpty(requests)){
            for(CertificateRequest request : requests){
                request.setAccount(null);
            }
        }

        Set<ServerEntry> serverEntries = getServerEntries();
        if(!CollectionUtils.isEmpty(serverEntries)){
            for(ServerEntry serverEntry : serverEntries){
                serverEntry.setAccount(null);
            }
        }

        Set<PocEntry> pocEntries = getPocs();
        if(!CollectionUtils.isEmpty(pocEntries)){
            for(PocEntry pocEntry : pocEntries){
                pocEntry.setAccount(null);
            }
        }

        Set<DomainPolicy> domainPolicies = getAccountDomainPolicies();
        if(!CollectionUtils.isEmpty(domainPolicies)){
            for(DomainPolicy dp : domainPolicies){
                dp.setAccount(null);
            }
        }

        Set<IssuedCertificate> issuedCertificates = getIssuedCertificates();
        if(!CollectionUtils.isEmpty(issuedCertificates)){
            for(IssuedCertificate issuedCertificate : issuedCertificates){
                issuedCertificate.setAccount(null);
            }
        }

        Set<AttributePolicyGroup> attributePolicyGroups = getPolicyGroups();
        if(!CollectionUtils.isEmpty(attributePolicyGroups)){
            for(AttributePolicyGroup group : attributePolicyGroups){
                group.setAccount(null);
            }
        }

        if(!CollectionUtils.isEmpty(authCredentials)){
            for(AuthCredential credential : authCredentials){
                credential.setParentEntity(null);
            }
        }
    }

    public String getKeyIdentifier() {
        return keyIdentifier;
    }

    public void setKeyIdentifier(String keyIdentifier) {
        this.keyIdentifier = keyIdentifier;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<PocEntry> getPocs() {
        if(pocs == null) pocs = new HashSet<>();
        return pocs;
    }

    public void setPocs(Set<PocEntry> pocs) {
        this.pocs = pocs;
    }

    public Set<AccountRestriction> getAccountRestrictions() {
        if(accountRestrictions == null) accountRestrictions = new HashSet<>();
        return accountRestrictions;
    }

    public void setAccountRestrictions(Set<AccountRestriction> accountRestrictions) {
        this.getAccountRestrictions().clear();
        this.getAccountRestrictions().addAll(accountRestrictions);
    }

    public void addPoc(PocEntry pocEntry){
        getPocs().add(pocEntry);
    }

    public Set<CertificateRequest> getCertificateRequests() {
        if(certificateRequests == null) certificateRequests = new HashSet<>();
        return certificateRequests;
    }

    public void setCertificateRequests(Set<CertificateRequest> certificateRequests) {
        this.certificateRequests = certificateRequests;
    }

    public Set<ServerEntry> getServerEntries() {
        if(serverEntries == null) serverEntries = new HashSet<>();
        return serverEntries;
    }

    public void setServerEntries(Set<ServerEntry> serverEntries) {
        this.serverEntries = serverEntries;
    }

    public Set<IssuedCertificate> getIssuedCertificates() {
        if(issuedCertificates == null) issuedCertificates = new HashSet<>();
        return issuedCertificates;
    }

    public void setIssuedCertificates(Set<IssuedCertificate> issuedCertificates) {
        this.issuedCertificates = issuedCertificates;
    }

    public Set<AttributePolicyGroup> getPolicyGroups() {
        if(policyGroups == null) policyGroups = new HashSet<>();
        return policyGroups;
    }

    public void setPolicyGroups(Set<AttributePolicyGroup> policyGroups) {
        this.policyGroups = policyGroups;
    }

    public Set<DomainPolicy> getAccountDomainPolicies() {
        if(accountDomainPolicies == null) accountDomainPolicies = new HashSet<>();
        return accountDomainPolicies;
    }

    public void setAccountDomainPolicies(Set<DomainPolicy> accountDomainPolicies) {
        this.accountDomainPolicies = accountDomainPolicies;
    }

    public String getSecurityPolicyServerProjectId() {
        return securityPolicyServerProjectId;
    }

    public void setSecurityPolicyServerProjectId(String securityPolicyServerProjectId) {
        this.securityPolicyServerProjectId = securityPolicyServerProjectId;
    }

    public String getEntityBaseDn() {
        return entityBaseDn;
    }

    public void setEntityBaseDn(String entityBaseDn) {
        this.entityBaseDn = entityBaseDn;
    }

    //@JsonIgnore
    //public String getMacKeyBase64(){
    //    return Base64.encode(this.macKey).toString();
    //}

    public boolean isAllowHostnameIssuance() {
        return allowHostnameIssuance;
    }

    public void setAllowHostnameIssuance(boolean allowHostnameIssuance) {
        this.allowHostnameIssuance = allowHostnameIssuance;
    }

    @Override
    public Set<AuthCredential> getAuthCredentials() {
        if(authCredentials == null) authCredentials = new HashSet<>();
        return authCredentials;
    }

    public void setAuthCredentials(Set<AuthCredential> authCredentials) {
        this.authCredentials = authCredentials;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Account account = (Account) o;
        return Objects.equals(keyIdentifier, account.keyIdentifier) &&
                Objects.equals(projectName, account.projectName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), keyIdentifier, projectName);
    }

    @Override
    @JsonIgnore
    public Account getAccount() {
        return this;
    }

    @JsonIgnore
    @Override
    public Set<DomainPolicy> getDomainIssuanceRestrictions() {
        return getAccountDomainPolicies();
    }
}
