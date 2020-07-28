package com.winllc.pki.ra.ca;

import com.winllc.acme.common.CertSearchParam;
import com.winllc.acme.common.CertificateDetails;
import com.winllc.acme.common.SubjectAltNames;
import com.winllc.pki.ra.domain.CertAuthorityConnectionInfo;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

public interface CertAuthority {
    CertAuthorityConnectionType getType();
    String getName();
    CertAuthorityConnectionInfo getConnectionInfo();
    X509Certificate issueCertificate(String csr, String dn, SubjectAltNames sans) throws Exception;
    boolean revokeCertificate(String serial, int reason) throws Exception;
    String getCertificateStatus(String serial) throws Exception;
    List<CertificateDetails> search(CertSearchParam params);
    Certificate[] getTrustChain() throws Exception;
    X509Certificate getCertificateBySerial(String serial) throws Exception;
}
