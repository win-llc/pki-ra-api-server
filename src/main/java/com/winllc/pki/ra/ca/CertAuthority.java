package com.winllc.pki.ra.ca;

import com.winllc.acme.common.CertSearchParam;
import com.winllc.acme.common.CertificateDetails;
import com.winllc.acme.common.SubjectAltNames;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

public interface CertAuthority {
    CertAuthorityConnectionType getType();
    List<String> getRequiredConnectionProperties();
    String getName();
    X509Certificate issueCertificate(String csr, SubjectAltNames sans);
    boolean revokeCertificate(String serial, int reason);
    String getCertificateStatus(String serial);
    List<CertificateDetails> search(CertSearchParam params);
    Certificate[] getTrustChain();
    X509Certificate getCertificateBySerial(String serial);
}
