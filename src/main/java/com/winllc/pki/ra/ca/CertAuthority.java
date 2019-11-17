package com.winllc.pki.ra.ca;

import org.springframework.web.bind.annotation.PathVariable;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public interface CertAuthority {
    String getName();
    X509Certificate issueCertificate(String csr);
    boolean revokeCertificate(String serial, int reason);
    String getCertificateStatus(String serial);
    Certificate[] getTrustChain();
    X509Certificate getCertificateBySerial(String serial);
}
