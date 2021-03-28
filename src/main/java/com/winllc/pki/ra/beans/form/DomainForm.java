package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.util.FormValidationUtil;

import javax.validation.constraints.NotEmpty;
import java.util.regex.Pattern;

public class DomainForm extends ValidForm<Domain> {

    @NotEmpty
    private String base;
    private Long parentDomainId;
    private String parentDomainBase;

    public DomainForm(Domain entity) {
        super(entity);
        this.base = entity.getBase();

        if(entity.getParentDomain() != null){
            this.parentDomainId = entity.getParentDomain().getId();
            this.parentDomainBase = entity.getParentDomain().getFullDomainName();
        }
    }

    public DomainForm(String base) {
        this.base = base;
    }

    private DomainForm(){}

    @Override
    protected void processIsValid() {
        if(!FormValidationUtil.isValidFqdn(base)){
            getErrors().put("invalidBase", "Invalid base");
        }
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public Long getParentDomainId() {
        return parentDomainId;
    }

    public void setParentDomainId(Long parentDomainId) {
        this.parentDomainId = parentDomainId;
    }

    public String getParentDomainBase() {
        return parentDomainBase;
    }

    public void setParentDomainBase(String parentDomainBase) {
        this.parentDomainBase = parentDomainBase;
    }
}
