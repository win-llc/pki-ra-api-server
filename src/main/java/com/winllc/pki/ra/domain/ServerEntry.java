package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;
import org.springframework.ldap.odm.annotations.Transient;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.naming.Name;
import javax.persistence.*;
import java.util.*;

@Entity
@Entry(objectClasses = {"top", "untypedObject"})
@Table(name = "server_entry")
public class ServerEntry extends AuthCredentialHolder implements AccountOwnedEntity {

    //allow pre-authz tracking per account

    @Id
    @javax.persistence.Transient
    private Name dn;
    @Transient
    private String hostname;
    @Attribute(name = "cn")
    @EntityVariableField
    private String fqdn;
    @Transient
    private String distinguishedName;
    @JsonIgnore
    @ElementCollection
    @Transient
    private List<String> alternateDnsValues = new ArrayList<>();
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name="domainParent_fk")
    @Transient
    private Domain domainParent;
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name="account_fk")
    @Transient
    private Account account;
    @JsonIgnore
    @OneToMany(mappedBy = "serverEntry")
    @Transient
    private Set<CertificateRequest> certificateRequests;
    @JsonIgnore
    @OneToMany(mappedBy = "parentEntity")
    @Transient
    private Set<AuthCredential> authCredentials;
    @Transient
    private String openidClientId;
    @Transient
    private String openidClientRedirectUrl;
    @Transient
    private Boolean acmeAllowPreAuthz;

    @ManyToMany(mappedBy = "manages", fetch = FetchType.LAZY)
    private Set<PocEntry> managedBy;


    public static ServerEntry buildNew(){
        ServerEntry serverEntry = new ServerEntry();
        serverEntry.setUuid(UUID.randomUUID().toString());
        return serverEntry;
    }

    @PreRemove
    private void preRemove(){
        if(domainParent != null){
            setDomainParent(null);
        }

        if(account != null){
            Set<ServerEntry> serverEntries = account.getServerEntries();
            if(!CollectionUtils.isEmpty(serverEntries)){
                account.getServerEntries().remove(this);
            }
        }

        if(!CollectionUtils.isEmpty(certificateRequests)){
            for(CertificateRequest request : certificateRequests){
                request.setServerEntry(null);
            }
        }

        if(!CollectionUtils.isEmpty(authCredentials)){
            for(AuthCredential credential : authCredentials){
                credential.setParentEntity(null);
            }
        }

        if(!CollectionUtils.isEmpty(managedBy)){
            for(PocEntry pocEntry : managedBy){
                pocEntry.getManages().remove(this);
            }
        }
    }

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

    public Set<CertificateRequest> getCertificateRequests() {
        if(certificateRequests == null) certificateRequests = new HashSet<>();
        return certificateRequests;
    }

    public void setCertificateRequests(Set<CertificateRequest> certificateRequests) {
        this.certificateRequests = certificateRequests;
    }

    public String getDistinguishedName() {
        return distinguishedName;
    }

    public void setDistinguishedName(String distinguishedName) {
        this.distinguishedName = distinguishedName;
    }

    public Boolean getAcmeAllowPreAuthz() {
        return acmeAllowPreAuthz;
    }

    public void setAcmeAllowPreAuthz(Boolean acmeAllowPreAuthz) {
        this.acmeAllowPreAuthz = acmeAllowPreAuthz;
    }

    public Set<AuthCredential> getAuthCredentials() {
        if(authCredentials == null) authCredentials = new HashSet<>();
        return authCredentials;
    }

    public void setAuthCredentials(Set<AuthCredential> authCredentials) {
        this.authCredentials = authCredentials;
    }

    public Name getDn() {
        return dn;
    }

    public void setDn(Name dn) {
        this.dn = dn;
    }

    public Set<PocEntry> getManagedBy() {
        return managedBy;
    }

    public void setManagedBy(Set<PocEntry> managedBy) {
        this.managedBy = managedBy;
    }

    public Name buildDn(String baseDn){
        LdapNameBuilder builder;
        if(baseDn != null && !StringUtils.isEmpty(baseDn)){
            builder = LdapNameBuilder.newInstance(baseDn);
        }else{
            builder = LdapNameBuilder.newInstance();
        }

        this.dn = builder
                .add("cn", fqdn)
                .build();
        this.distinguishedName = this.dn.toString();
        return this.dn;
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
