package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.domain.Domain;

import javax.validation.constraints.NotEmpty;

public class DomainForm extends ValidForm<Domain> {

    @NotEmpty
    private String base;
    private Long parentDomainId;

    public DomainForm(Domain entity) {
        super(entity);
        this.base = entity.getBase();

        if(entity.getParentDomain() != null){
            this.parentDomainId = entity.getParentDomain().getId();
        }
    }

    public DomainForm(String base) {
        this.base = base;
    }

    private DomainForm(){}

    @Override
    protected void processIsValid() {
        //todo
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
}
