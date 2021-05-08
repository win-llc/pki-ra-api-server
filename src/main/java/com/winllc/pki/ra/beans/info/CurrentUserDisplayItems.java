package com.winllc.pki.ra.beans.info;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class CurrentUserDisplayItems {
    private int accountRequestsCount;
    private int domainLinkRequestsCount;
    private int manualCertRequestsCount;
    private int requestsTotal;

    private int notificationsCount;

    @JsonIgnore
    public void updateRequestCount(){
        setRequestsTotal(accountRequestsCount + domainLinkRequestsCount + manualCertRequestsCount);
    }

    public int getAccountRequestsCount() {
        return accountRequestsCount;
    }

    public void setAccountRequestsCount(int accountRequestsCount) {
        this.accountRequestsCount = accountRequestsCount;
    }

    public int getDomainLinkRequestsCount() {
        return domainLinkRequestsCount;
    }

    public void setDomainLinkRequestsCount(int domainLinkRequestsCount) {
        this.domainLinkRequestsCount = domainLinkRequestsCount;
    }

    public int getManualCertRequestsCount() {
        return manualCertRequestsCount;
    }

    public void setManualCertRequestsCount(int manualCertRequestsCount) {
        this.manualCertRequestsCount = manualCertRequestsCount;
    }

    public int getRequestsTotal() {
        return requestsTotal;
    }

    public void setRequestsTotal(int requestsTotal) {
        this.requestsTotal = requestsTotal;
    }

    public int getNotificationsCount() {
        return notificationsCount;
    }

    public void setNotificationsCount(int notificationsCount) {
        this.notificationsCount = notificationsCount;
    }
}
