package com.winllc.pki.ra.beans;

import com.winllc.acme.common.CertSearchParam;
import com.winllc.acme.common.CertSearchParams;
import com.winllc.pki.ra.ca.CertAuthority;
import com.winllc.pki.ra.service.transaction.CertTransaction;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.ldap.LdapName;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

//todo flesh this out, cert interaction should be at object level
public class SystemCertificate {

    private Name dn;
    private X509Certificate x509Certificate;
    private CertAuthority certAuthority;

    public SystemCertificate build(X509Certificate x509Certificate, CertAuthority certAuthority) throws InvalidNameException {
        SystemCertificate systemCertificate = new SystemCertificate(x509Certificate);
        systemCertificate.certAuthority = certAuthority;
        return systemCertificate;
    }

    private SystemCertificate(X509Certificate x509Certificate) throws InvalidNameException {
        this.x509Certificate = x509Certificate;
        this.dn = new LdapName(x509Certificate.getSubjectDN().getName());
    }

    public void revoke(int reason) throws Exception {
        certAuthority.revokeCertificate(x509Certificate.getSerialNumber().toString(), reason);
    }

    private void sync() throws Exception {
        CertSearchParam param = CertSearchParam.createNew()
                .field(CertSearchParams.CertField.SERIAL)
                .value(x509Certificate.getSerialNumber().toString())
                .relation(CertSearchParams.CertSearchParamRelation.EQUALS);

        X509Certificate cert = certAuthority.getCertificateBySerial(x509Certificate.getSerialNumber().toString());

    }
}
