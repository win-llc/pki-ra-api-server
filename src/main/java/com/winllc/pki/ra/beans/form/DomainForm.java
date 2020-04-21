package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.domain.Domain;

import javax.validation.constraints.NotEmpty;

public class DomainForm extends ValidForm<Domain> {

    @NotEmpty
    private String base;

    public DomainForm(Domain entity) {
        super(entity);
        this.base = entity.getBase();
    }

    private DomainForm() {
    }

    @Override
    protected void processIsValid() {

    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }
}
