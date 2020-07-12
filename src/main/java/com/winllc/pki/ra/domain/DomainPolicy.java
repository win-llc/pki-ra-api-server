package com.winllc.pki.ra.domain;

import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PreRemove;

@Entity
public class DomainPolicy extends AbstractPersistable<Long> {

    @ManyToOne
    @JoinColumn(name="targetDomain_fk")
    private Domain targetDomain;
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
}
