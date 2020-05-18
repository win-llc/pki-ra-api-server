package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class AttributePolicyGroup extends AbstractPersistable<Long> {

    private String name;
    @JsonIgnore
    @OneToMany(mappedBy = "attributePolicyGroup")
    private Set<AttributePolicy> attributePolicies;
    @JsonIgnore
    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE
    })
    @JoinTable(name = "attributePolicyGroup_serverEntry",
            joinColumns = @JoinColumn(name = "attributePolicyGroup_id"),
            inverseJoinColumns = @JoinColumn(name = "serverEntry_id")
    )
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
