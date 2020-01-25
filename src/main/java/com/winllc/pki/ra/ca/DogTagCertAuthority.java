package com.winllc.pki.ra.ca;

import com.winllc.acme.common.CertSearchParam;
import com.winllc.acme.common.CertificateDetails;
import com.winllc.acme.common.SubjectAltNames;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

public class DogTagCertAuthority implements CertAuthority {
    @Override
    public CertAuthorityConnectionType getType() {
        return CertAuthorityConnectionType.DOGTAG;
    }

    @Override
    public List<String> getRequiredConnectionProperties() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public X509Certificate issueCertificate(String csr, SubjectAltNames sans) {

        return null;
    }

    @Override
    public boolean revokeCertificate(String serial, int reason) {
        return false;
    }

    @Override
    public String getCertificateStatus(String serial) {
        return null;
    }

    @Override
    public List<CertificateDetails> search(CertSearchParam params) {
        return null;
    }

    @Override
    public Certificate[] getTrustChain() {
        return new Certificate[0];
    }

    @Override
    public X509Certificate getCertificateBySerial(String serial) {
        return null;
    }
}
