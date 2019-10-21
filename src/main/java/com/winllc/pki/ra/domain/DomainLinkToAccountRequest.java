package com.winllc.pki.ra.domain;

import javax.persistence.Entity;

@Entity
public class DomainLinkToAccountRequest extends BaseEntity {

    private Long accountId;
    private Long requestedDomainId;
    private String status;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setStatusRequested(){
        setStatus("requested");
    }

    public void setStatusRejected(){
        setStatus("rejected");
    }

    public void setStatusApproved(){
        setStatus("approved");
    }

    public Long getRequestedDomainId() {
        return requestedDomainId;
    }

    public void setRequestedDomainId(Long requestedDomainId) {
        this.requestedDomainId = requestedDomainId;
    }
}
