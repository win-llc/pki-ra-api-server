package com.winllc.pki.ra.beans;

import com.winllc.acme.common.SubjectAltName;

import java.util.List;

public class CertificateRequestForm {

    private String csr;
    private String name;
    private String certAuthorityName;
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
}
