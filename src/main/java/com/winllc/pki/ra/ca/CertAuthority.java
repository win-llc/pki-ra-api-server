package com.winllc.pki.ra.ca;

import org.springframework.web.bind.annotation.PathVariable;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public interface CertAuthority {
    X509Certificate issueCertificate(String csr);
    boolean revokeCertificate(String serial, int reason);
    Certificate[] getTrustChain();
    X509Certificate getCertificateBySerial(String serial);
}
