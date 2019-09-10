package com.winllc.pki.ra.domain;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Domain extends BaseEntity {

    private String base;
    @ManyToMany
    private Set<Account> canIssueAccounts;

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
