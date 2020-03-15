package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.domain.AttributePolicy;
import com.winllc.pki.ra.domain.AttributePolicyGroup;

import java.util.ArrayList;
import java.util.List;

public class AttributePolicyGroupForm extends ValidForm<AttributePolicyGroup> {
    //todo

    private String name;
    private List<AttributePolicy> attributePolicies;

    public AttributePolicyGroupForm(AttributePolicyGroup attributePolicyGroup){
        super(attributePolicyGroup);
        this.name = attributePolicyGroup.getName();
        this.attributePolicies = new ArrayList<>(attributePolicyGroup.getAttributePolicies());
    }

    private AttributePolicyGroupForm(){}

    @Override
    protected void processIsValid() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<AttributePolicy> getAttributePolicies() {
        return attributePolicies;
    }

    public void setAttributePolicies(List<AttributePolicy> attributePolicies) {
        this.attributePolicies = attributePolicies;
    }
}
