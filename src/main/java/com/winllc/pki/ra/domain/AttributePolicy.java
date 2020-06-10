package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PreRemove;

@Entity
public class AttributePolicy extends AbstractPersistable<Long> {

    private String attributeName;
    private String attributeValue;
    private boolean multiValued;
    private boolean staticValue;
    @ManyToOne
    @JsonIgnore
    @JoinColumn(name="attributePolicyGroup_fk")
    private AttributePolicyGroup attributePolicyGroup;

    //if value from security policy is true, check if security policy service associated with the AttributePolicyGroup
    //contains a matching key-value, if yes apply the value
    private boolean valueFromSecurityPolicy;
    private String securityAttributeKeyName;
    private String securityAttributeValue;

    @PreRemove
    private void preRemove(){
        if(attributePolicyGroup != null){
            attributePolicyGroup.getAttributePolicies().remove(this);
        }
    }

    @JsonIgnore
    public boolean isVariableValue(){
        return attributeValue != null && attributeValue.startsWith("{") && attributeValue.endsWith("}");
    }

    @JsonIgnore
    public String getVariableValueField(){
        return attributeValue.replace("{","").replace("}","");
    }

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

    public boolean isStaticValue() {
        return staticValue;
    }

    public void setStaticValue(boolean staticValue) {
        this.staticValue = staticValue;
    }

    public boolean isValueFromSecurityPolicy() {
        return valueFromSecurityPolicy;
    }

    public void setValueFromSecurityPolicy(boolean valueFromSecurityPolicy) {
        this.valueFromSecurityPolicy = valueFromSecurityPolicy;
    }

    public String getSecurityAttributeKeyName() {
        return securityAttributeKeyName;
    }

    public void setSecurityAttributeKeyName(String securityAttributeKeyName) {
        this.securityAttributeKeyName = securityAttributeKeyName;
    }

    public String getSecurityAttributeValue() {
        return securityAttributeValue;
    }

    public void setSecurityAttributeValue(String securityAttributeValue) {
        this.securityAttributeValue = securityAttributeValue;
    }
}
