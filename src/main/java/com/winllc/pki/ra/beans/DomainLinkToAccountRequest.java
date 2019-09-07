package com.winllc.pki.ra.beans;

import java.util.List;

public class DomainLinkToAccountRequest {

    private String accountId;
    private List<String> requestedDomains;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public List<String> getRequestedDomains() {
        return requestedDomains;
    }

    public void setRequestedDomains(List<String> requestedDomains) {
        this.requestedDomains = requestedDomains;
    }
}
