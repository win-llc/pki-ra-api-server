package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winllc.acme.common.domain.BaseEntity;
import org.springframework.util.CollectionUtils;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "attribute_policy_group")
public class AttributePolicyGroup extends BaseEntity {

    private String name;
    @JsonIgnore
    @OneToMany(mappedBy = "attributePolicyGroup", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private Set<AttributePolicy> attributePolicies;
    @ManyToOne
    @JoinColumn(name="account_fk")
    private Account account;
    @ManyToOne
    @JoinColumn(name="ldapSchemaOverlay_fk")
    private LdapSchemaOverlay ldapSchemaOverlay;

    @PreRemove
    private void preRemove(){
        if(account != null && !CollectionUtils.isEmpty(account.getPolicyGroups())){
            account.getPolicyGroups().remove(this);
        }

        for(AttributePolicy attributePolicy : getAttributePolicies()){
            attributePolicy.setAttributePolicyGroup(null);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LdapSchemaOverlay getLdapSchemaOverlay() {
        return ldapSchemaOverlay;
    }

    public void setLdapSchemaOverlay(LdapSchemaOverlay ldapSchemaOverlay) {
        this.ldapSchemaOverlay = ldapSchemaOverlay;
    }

    public Set<AttributePolicy> getAttributePolicies() {
        if(attributePolicies == null) attributePolicies = new HashSet<>();
        return attributePolicies;
    }

    public void setAttributePolicies(Set<AttributePolicy> attributePolicies) {
        this.attributePolicies = attributePolicies;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
