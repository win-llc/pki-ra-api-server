package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

@Entity
public class AttributePolicyGroup extends AbstractPersistable<Long> {

    private String name;
    @JsonIgnore
    @OneToMany(mappedBy = "attributePolicyGroup")
    private Set<AttributePolicy> attributePolicies;
    @JsonIgnore
    @ManyToMany
    private Set<ServerEntry> serverEntries;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<AttributePolicy> getAttributePolicies() {
        return attributePolicies;
    }

    public void setAttributePolicies(Set<AttributePolicy> attributePolicies) {
        this.attributePolicies = attributePolicies;
    }

    public Set<ServerEntry> getServerEntries() {
        if(serverEntries == null) serverEntries = new HashSet<>();
        return serverEntries;
    }

    public void setServerEntries(Set<ServerEntry> serverEntries) {
        this.serverEntries = serverEntries;
    }
}
