package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.util.CollectionUtils;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Domain extends AbstractPersistable<Long>  {

    @Column(unique = true, nullable = false)
    private String base;
    @JsonIgnore
    @OneToMany(mappedBy = "parentDomain")
    private Set<Domain> subDomains;
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name="parentDomain_fk")
    private Domain parentDomain;
    /*
    @JsonIgnore
    @ManyToMany(mappedBy = "canIssueDomains")
    private Set<Account> canIssueAccounts;
     */
    @JsonIgnore
    @OneToMany(mappedBy = "domainParent")
    private Set<ServerEntry> serverEntries;
    @OneToOne
    private DomainPolicy globalDomainPolicy;
    //@JsonIgnore
    //@OneToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    //private Set<DomainCertIssuanceRestriction> globalDomainCertIssuanceRestrictions;

    @JsonIgnore
    @OneToMany(mappedBy = "targetDomain", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private Set<DomainPolicy> allDomainPolicies;

    @PreRemove
    private void preRemove() {
        /*
        Set<Account> accounts = getCanIssueAccounts();
        if (!CollectionUtils.isEmpty(accounts)) {
            for (Account account : accounts) {
                account.getCanIssueDomains().remove(this);
            }
        }
         */

        Set<ServerEntry> serverEntries = getServerEntries();
        if(!CollectionUtils.isEmpty(serverEntries)){
            for(ServerEntry serverEntry : serverEntries){
                serverEntry.setDomainParent(null);
            }
        }

        Set<DomainPolicy> restrictions = getAllDomainPolicies();
        if(!CollectionUtils.isEmpty(restrictions)){
            for(DomainPolicy restriction : restrictions){
                restriction.setTargetDomain(null);
            }
        }
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    /*
    public Set<Account> getCanIssueAccounts() {
        if(canIssueAccounts == null) canIssueAccounts = new HashSet<>();
        return canIssueAccounts;
    }

    public void setCanIssueAccounts(Set<Account> canIssueAccounts) {
        this.canIssueAccounts = canIssueAccounts;
    }
     */

    public Set<ServerEntry> getServerEntries() {
        return serverEntries;
    }

    public void setServerEntries(Set<ServerEntry> serverEntries) {
        this.serverEntries = serverEntries;
    }

    public Set<DomainPolicy> getAllDomainPolicies() {
        if(allDomainPolicies == null) allDomainPolicies = new HashSet<>();
        return allDomainPolicies;
    }

    public void setAllDomainPolicies(Set<DomainPolicy> allDomainPolicies) {
        this.allDomainPolicies = allDomainPolicies;
    }

    public Set<Domain> getSubDomains() {
        if(subDomains == null) subDomains = new HashSet<>();
        return subDomains;
    }

    public void setSubDomains(Set<Domain> subDomains) {
        this.subDomains = subDomains;
    }

    public Domain getParentDomain() {
        return parentDomain;
    }

    public void setParentDomain(Domain parentDomain) {
        this.parentDomain = parentDomain;
    }

    public DomainPolicy getGlobalDomainPolicy() {
        return globalDomainPolicy;
    }

    public void setGlobalDomainPolicy(DomainPolicy globalDomainPolicy) {
        this.globalDomainPolicy = globalDomainPolicy;
    }
}
