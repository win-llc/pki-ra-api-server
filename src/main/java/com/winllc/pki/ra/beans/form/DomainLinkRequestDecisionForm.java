package com.winllc.pki.ra.beans.form;

public class DomainLinkRequestDecisionForm {
    private Long requestId;
    private String status;

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
