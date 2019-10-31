package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.HashSet;
import java.util.Set;

@Entity
public class DomainLinkToAccountRequest extends BaseEntity {

    private Long accountId;
    @ElementCollection
    @JsonIgnore
    private Set<Long> requestedDomainIds = new HashSet<>();
    private String status;

    public static DomainLinkToAccountRequest buildNew(){
        DomainLinkToAccountRequest request = new DomainLinkToAccountRequest();
        request.setStatusRequested();
        return request;
    }

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

    public Set<Long> getRequestedDomainIds() {
        return requestedDomainIds;
    }

    public void setRequestedDomainIds(Set<Long> requestedDomainIds) {
        this.requestedDomainIds = requestedDomainIds;
    }

    public void setStatusRequested(){
        setStatus("new");
    }

    public void setStatusRejected(){
        setStatus("rejected");
    }

    public void setStatusApproved(){
        setStatus("approved");
    }

    @Override
    public String toString() {
        return "DomainLinkToAccountRequest{" +
                "accountId=" + accountId +
                ", requestedDomainIds=" + requestedDomainIds +
                ", status='" + status + '\'' +
                "} " + super.toString();
    }
}
