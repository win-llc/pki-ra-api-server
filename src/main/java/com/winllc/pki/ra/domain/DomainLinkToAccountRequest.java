package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "domain_link_to_account_request")
public class DomainLinkToAccountRequest extends AbstractPersistable<Long> {

    @Column(nullable = false)
    private Long accountId;
    @ElementCollection
    @CollectionTable(name="domainLinkToAccountRequest_requestedDomainIds", joinColumns = @JoinColumn(name = "request_id"))
    @JsonIgnore
    private Set<Long> requestedDomainIds = new HashSet<>();
    @Column(nullable = false)
    private String status;

    private Timestamp statusUpdatedOn;
    private String decisionMadeBy;

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

    public Timestamp getStatusUpdatedOn() {
        return statusUpdatedOn;
    }

    public void setStatusUpdatedOn(Timestamp statusUpdatedOn) {
        this.statusUpdatedOn = statusUpdatedOn;
    }

    public String getDecisionMadeBy() {
        return decisionMadeBy;
    }

    public void setDecisionMadeBy(String decisionMadeBy) {
        this.decisionMadeBy = decisionMadeBy;
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
