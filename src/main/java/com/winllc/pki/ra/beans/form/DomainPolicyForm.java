package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.domain.CertificateRequest;
import com.winllc.acme.common.domain.DomainPolicy;

import java.util.ArrayList;
import java.util.List;

public class DomainPolicyForm extends ValidForm<DomainPolicy> {

    private Long domainId;
    private String domainName;
    private boolean allowIssuance = true;
    private boolean acmeRequireHttpValidation = false;
    private boolean acmeRequireDnsValidation = false;
    private List<DomainPolicyForm> subDomainForms;

    public DomainPolicyForm(DomainPolicy restriction){
        super(restriction);
        this.domainId = restriction.getTargetDomain().getId();
        this.domainName = restriction.getTargetDomain().getFullDomainName();
        this.acmeRequireDnsValidation = restriction.isAcmeRequireDnsValidation();
        this.acmeRequireHttpValidation = restriction.isAcmeRequireHttpValidation();
        this.allowIssuance = restriction.isAllowIssuance();
    }

    public DomainPolicyForm(){}

    @Override
    protected void processIsValid() {

    }

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
