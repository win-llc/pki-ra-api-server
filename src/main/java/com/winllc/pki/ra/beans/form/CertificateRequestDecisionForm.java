package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.constants.CertificateRequestAction;
import com.winllc.acme.common.domain.CertificateRequest;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class CertificateRequestDecisionForm extends ValidForm<CertificateRequest> {

    @NotNull
    private Long requestId;
    @NotEmpty(message = "Status must not be empty")
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
    protected void processIsValid() {
        try {
            CertificateRequestAction.valueOf(status);
        }catch (Exception e){
            errors.put("status", "Invalid status");
        }
    }
}
