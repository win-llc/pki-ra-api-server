package com.winllc.pki.ra.domain;

import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
public class AttributePolicy extends AbstractPersistable<Long> {

    private String attributeName;
    private String attributeValue;
    private boolean multiValued;
    @ManyToOne
    private AttributePolicyGroup attributePolicyGroup;

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getAttributeValue() {
        return attributeValue;
    }

    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    public boolean isMultiValued() {
        return multiValued;
    }

    public void setMultiValued(boolean multiValued) {
        this.multiValued = multiValued;
    }

    public AttributePolicyGroup getAttributePolicyGroup() {
        return attributePolicyGroup;
    }

    public void setAttributePolicyGroup(AttributePolicyGroup attributePolicyGroup) {
        this.attributePolicyGroup = attributePolicyGroup;
    }
}
