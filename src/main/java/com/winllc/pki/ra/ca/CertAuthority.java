package com.winllc.pki.ra.ca;

import com.winllc.acme.common.CertSearchParams;
import com.winllc.acme.common.CertificateDetails;
import com.winllc.acme.common.SubjectAltNames;
import org.springframework.web.bind.annotation.PathVariable;
import sun.security.x509.SubjectAlternativeNameExtension;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

public interface CertAuthority {
    String getName();
    X509Certificate issueCertificate(String csr, SubjectAltNames sans);
    boolean revokeCertificate(String serial, int reason);
    String getCertificateStatus(String serial);
    List<CertificateDetails> search(CertSearchParams params);
    Certificate[] getTrustChain();
    X509Certificate getCertificateBySerial(String serial);
}
