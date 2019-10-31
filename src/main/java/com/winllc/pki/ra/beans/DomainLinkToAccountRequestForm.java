package com.winllc.pki.ra.beans;

import java.util.List;

public class DomainLinkToAccountRequestForm {

    private Long accountId;
    private List<Long> requestedDomainIds;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public List<Long> getRequestedDomainIds() {
        return requestedDomainIds;
    }

    public void setRequestedDomainIds(List<Long> requestedDomainIds) {
        this.requestedDomainIds = requestedDomainIds;
    }
}
