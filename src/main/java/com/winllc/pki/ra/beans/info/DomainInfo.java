package com.winllc.pki.ra.beans.info;

import com.winllc.pki.ra.domain.Domain;

import java.util.List;
import java.util.stream.Collectors;

public class DomainInfo extends InfoObject<Domain> {

    private String base;
    private List<AccountInfo> accountsThatCanIssue;

    public DomainInfo(Domain domain, boolean loadAccountInfo){
        super(domain);
        this.base = domain.getBase();
        if(loadAccountInfo) {
            this.accountsThatCanIssue = domain.getCanIssueAccounts().stream()
                    .map(a -> new AccountInfo(a, false))
                    .collect(Collectors.toList());
        }
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public List<AccountInfo> getAccountsThatCanIssue() {
        return accountsThatCanIssue;
    }

    public void setAccountsThatCanIssue(List<AccountInfo> accountsThatCanIssue) {
        this.accountsThatCanIssue = accountsThatCanIssue;
    }
}
