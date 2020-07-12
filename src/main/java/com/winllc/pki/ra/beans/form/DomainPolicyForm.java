package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.domain.DomainPolicy;

import java.util.ArrayList;
import java.util.List;

public class DomainPolicyForm {

    private Long domainId;
    private String domainName;
    private boolean allowIssuance = true;
    private boolean acmeRequireHttpValidation = false;
    private boolean acmeRequireDnsValidation = false;
    private List<DomainPolicyForm> subDomainForms;

    public DomainPolicyForm(DomainPolicy restriction){
        this.domainId = restriction.getTargetDomain().getId();
        this.domainName = restriction.getTargetDomain().getBase();
        this.acmeRequireDnsValidation = restriction.isAcmeRequireDnsValidation();
        this.acmeRequireHttpValidation = restriction.isAcmeRequireHttpValidation();
        this.allowIssuance = restriction.isAllowIssuance();
    }

    private DomainPolicyForm(){}

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
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

    public List<DomainPolicyForm> getSubDomainForms() {
        if(subDomainForms == null) subDomainForms = new ArrayList<>();
        return subDomainForms;
    }

    public void setSubDomainForms(List<DomainPolicyForm> subDomainForms) {
        this.subDomainForms = subDomainForms;
    }
}
