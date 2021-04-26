package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.ldap.odm.annotations.Transient;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "domain_policy")
public class DomainPolicy extends AuthCredentialHolder implements AccountOwnedEntity {

    @JsonIgnore
    @OneToMany(mappedBy = "parentEntity", fetch = FetchType.EAGER)
    @Transient
    private Set<AuthCredential> authCredentials;
    @ManyToOne
    @JoinColumn(name="targetDomain_fk")
    private Domain targetDomain;
    @ManyToOne
    @JoinColumn(name="account_fk")
    private Account account;

    private boolean allowIssuance = true;
    private boolean acmeRequireHttpValidation = false;
    private boolean acmeRequireDnsValidation = false;

    public DomainPolicy(){}

    public DomainPolicy(Domain domain){
        this.targetDomain = domain;
    }

    @PreRemove
    private void preRemove(){
        if(targetDomain != null) {
            targetDomain.getAllDomainPolicies().remove(this);
        }

        if(account != null){
            account.getAccountDomainPolicies().remove(this);
        }

    }

    public Domain getTargetDomain() {
        return targetDomain;
    }

    public void setTargetDomain(Domain targetDomain) {
        this.targetDomain = targetDomain;
    }

    public boolean isAllowIssuance() {
        return allowIssuance;
    }

    public void setAllowIssuance(boolean allowIssuance) {
        this.allowIssuance = allowIssuance;
    }

    public boolean isAcmeRequireHttpValidation() {
        return acmeRequireHttpValidation;
    }

    public void setAcmeRequireHttpValidation(boolean acmeRequireHttpValidation) {
        this.acmeRequireHttpValidation = acmeRequireHttpValidation;
    }

    public boolean isAcmeRequireDnsValidation() {
        return acmeRequireDnsValidation;
    }

    public void setAcmeRequireDnsValidation(boolean acmeRequireDnsValidation) {
        this.acmeRequireDnsValidation = acmeRequireDnsValidation;
    }

    public Set<AuthCredential> getAuthCredentials() {
        if(authCredentials == null) authCredentials = new HashSet<>();
        return authCredentials;
    }

    public void setAuthCredentials(Set<AuthCredential> authCredentials) {
        this.authCredentials = authCredentials;
    }

    @Override
    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
