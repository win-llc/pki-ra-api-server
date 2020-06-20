package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.util.CollectionUtils;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class AttributePolicyGroup extends AbstractPersistable<Long> {

    private String name;
    @JsonIgnore
    @OneToMany(mappedBy = "attributePolicyGroup", orphanRemoval = true)
    private Set<AttributePolicy> attributePolicies;
    @ManyToOne
    @JoinColumn(name="account_fk")
    private Account account;
    private String securityPolicyServiceName;

    @PreRemove
    private void preRemove(){
        if(account != null && !CollectionUtils.isEmpty(account.getPolicyGroups())){
            account.getPolicyGroups().remove(this);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<AttributePolicy> getAttributePolicies() {
        if(attributePolicies == null) attributePolicies = new HashSet<>();
        return attributePolicies;
    }

    public void setAttributePolicies(Set<AttributePolicy> attributePolicies) {
        this.attributePolicies = attributePolicies;
    }

    public String getSecurityPolicyServiceName() {
        return securityPolicyServiceName;
    }

    public void setSecurityPolicyServiceName(String securityPolicyServiceName) {
        this.securityPolicyServiceName = securityPolicyServiceName;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
