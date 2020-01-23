package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.domain.DomainLinkToAccountRequest;

import java.util.List;

public class DomainLinkToAccountRequestForm extends ValidForm<DomainLinkToAccountRequest> {

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

    @Override
    protected boolean isValid() {
        //todo
        return true;
    }
}
