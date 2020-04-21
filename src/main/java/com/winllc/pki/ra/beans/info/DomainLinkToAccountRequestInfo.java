package com.winllc.pki.ra.beans.info;

import com.winllc.pki.ra.domain.DomainLinkToAccountRequest;

import java.util.List;

public class DomainLinkToAccountRequestInfo extends InfoObject<DomainLinkToAccountRequest> {

    private String status;
    private List<DomainInfo> domainInfoList;
    private AccountInfo accountInfo;

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
}
