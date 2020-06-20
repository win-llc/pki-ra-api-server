package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.domain.AttributePolicy;
import com.winllc.pki.ra.domain.AttributePolicyGroup;

import java.util.ArrayList;
import java.util.List;

public class AttributePolicyGroupForm extends ValidForm<AttributePolicyGroup> {
    //todo

    private String name;
    private Long accountId;
    private List<AttributePolicy> attributePolicies;

    public AttributePolicyGroupForm(AttributePolicyGroup attributePolicyGroup){
        super(attributePolicyGroup);
        this.name = attributePolicyGroup.getName();
        this.accountId = attributePolicyGroup.getAccount().getId();
        this.attributePolicies = new ArrayList<>(attributePolicyGroup.getAttributePolicies());
    }

    public AttributePolicyGroupForm(){}

    @Override
    protected void processIsValid() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public List<AttributePolicy> getAttributePolicies() {
        if(attributePolicies == null) attributePolicies = new ArrayList<>();
        return attributePolicies;
    }

    public void setAttributePolicies(List<AttributePolicy> attributePolicies) {
        this.attributePolicies = attributePolicies;
    }
}
