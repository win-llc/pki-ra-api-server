package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.SubjectAltName;
import com.winllc.pki.ra.domain.CertificateRequest;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

public class CertificateRequestForm extends ValidForm<CertificateRequest> {

    private String csr;
    private String name;
    private String certAuthorityName;
    @NotNull
    private Long accountId;
    private List<SubjectAltName> requestedDnsNames;

    public String getCsr() {
        return csr;
    }

    public void setCsr(String csr) {
        this.csr = csr;
    }

    public List<SubjectAltName> getRequestedDnsNames() {
        return requestedDnsNames;
    }

    public void setRequestedDnsNames(List<SubjectAltName> requestedDnsNames) {
        this.requestedDnsNames = requestedDnsNames;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCertAuthorityName() {
        return certAuthorityName;
    }

    public void setCertAuthorityName(String certAuthorityName) {
        this.certAuthorityName = certAuthorityName;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }


    @Override
    protected void processIsValid() {
        //todo
    }
}
