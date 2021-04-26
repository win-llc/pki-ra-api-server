package com.winllc.pki.ra.beans.info;

import com.winllc.pki.ra.domain.DomainLinkToAccountRequest;

import java.sql.Timestamp;
import java.util.List;

public class DomainLinkToAccountRequestInfo extends InfoObject<DomainLinkToAccountRequest> {

    private String status;
    private List<DomainInfo> domainInfoList;
    private AccountInfo accountInfo;

    private String requestedOn;
    private String requestedBy;

    public DomainLinkToAccountRequestInfo(DomainLinkToAccountRequest request){
        super(request);
        this.status = request.getStatus();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<DomainInfo> getDomainInfoList() {
        return domainInfoList;
    }

    public void setDomainInfoList(List<DomainInfo> domainInfoList) {
        this.domainInfoList = domainInfoList;
    }

    public AccountInfo getAccountInfo() {
        return accountInfo;
    }

    public void setAccountInfo(AccountInfo accountInfo) {
        this.accountInfo = accountInfo;
    }

    public String getRequestedOn() {
        return requestedOn;
    }

    public void setRequestedOn(String requestedOn) {
        this.requestedOn = requestedOn;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }
}
