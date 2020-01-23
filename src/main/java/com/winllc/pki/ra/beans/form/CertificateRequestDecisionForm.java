package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.domain.CertificateRequest;

public class CertificateRequestDecisionForm extends ValidForm<CertificateRequest> {

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

    @Override
    protected boolean isValid() {
        return false;
    }
}
