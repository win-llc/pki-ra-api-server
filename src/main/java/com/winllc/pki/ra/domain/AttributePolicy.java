package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;

@Entity
@Table(name = "attribute_policy")
public class AttributePolicy extends AbstractPersistable<Long> {

    private String attributeName;
    private String attributeValue;
    @Column(columnDefinition="tinyint(1) default 0")
    private boolean multiValued = false;
    @Column(columnDefinition="tinyint(1) default 0")
    private boolean staticValue = false;
    @ManyToOne
    @JsonIgnore
    @JoinColumn(name="attributePolicyGroup_fk")
    private AttributePolicyGroup attributePolicyGroup;

    //if the security policy attribute exists, use the security policy value
    @Column(columnDefinition="tinyint(1) default 0")
    private boolean useSecurityAttributeValueIfNameExists = false;
    //if the security policy attribute and value match, use the above value
    @Column(columnDefinition="tinyint(1) default 0")
    private boolean useValueIfSecurityAttributeNameValueExists = false;
    private String securityAttributeKeyName;
    private String securityAttributeValue;

    @PreRemove
    private void preRemove(){
        if(attributePolicyGroup != null){
            attributePolicyGroup.getAttributePolicies().remove(this);
        }
    }

    @JsonIgnore
    public void update(AttributePolicy updated){
        setAttributeName(updated.getAttributeName());
        setAttributeValue(updated.getAttributeValue());
        setSecurityAttributeKeyName(updated.getSecurityAttributeKeyName());
        setSecurityAttributeValue(updated.getSecurityAttributeValue());
        setUseSecurityAttributeValueIfNameExists(updated.isUseSecurityAttributeValueIfNameExists());
        setUseValueIfSecurityAttributeNameValueExists(updated.isUseValueIfSecurityAttributeNameValueExists());
        setStaticValue(updated.isStaticValue());
    }

    @JsonIgnore
    public boolean isVariableValue(){
        return attributeValue != null && attributeValue.startsWith("{") && attributeValue.endsWith("}");
    }

    @JsonIgnore
    public String getVariableValueField(){
        return attributeValue.replace("{","").replace("}","");
    }

    @JsonIgnore
    public boolean checkSecurityPolicyBackedAttribute(){
        return useSecurityAttributeValueIfNameExists || useValueIfSecurityAttributeNameValueExists;
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

    public boolean isUseSecurityAttributeValueIfNameExists() {
        return useSecurityAttributeValueIfNameExists;
    }

    public void setUseSecurityAttributeValueIfNameExists(boolean useSecurityAttributeValueIfNameExists) {
        this.useSecurityAttributeValueIfNameExists = useSecurityAttributeValueIfNameExists;
    }

    public boolean isUseValueIfSecurityAttributeNameValueExists() {
        return useValueIfSecurityAttributeNameValueExists;
    }

    public void setUseValueIfSecurityAttributeNameValueExists(boolean useValueIfSecurityAttributeNameValueExists) {
        this.useValueIfSecurityAttributeNameValueExists = useValueIfSecurityAttributeNameValueExists;
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
