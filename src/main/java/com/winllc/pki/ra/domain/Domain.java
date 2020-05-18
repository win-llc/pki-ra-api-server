package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.util.CollectionUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.PreRemove;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Domain extends AbstractPersistable<Long> {

    @Column(unique = true, nullable = false)
    private String base;
    @JsonIgnore
    @ManyToMany(mappedBy = "canIssueDomains")
    private Set<Account> canIssueAccounts;

    @PreRemove
    private void preRemove() {
        Set<Account> accounts = getCanIssueAccounts();
        if (!CollectionUtils.isEmpty(accounts)) {
            for (Account account : accounts) {
                account.getCanIssueDomains().remove(this);
            }
        }
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public Set<Account> getCanIssueAccounts() {
        if(canIssueAccounts == null) canIssueAccounts = new HashSet<>();
        return canIssueAccounts;
    }

    public void setCanIssueAccounts(Set<Account> canIssueAccounts) {
        this.canIssueAccounts = canIssueAccounts;
    }
}
