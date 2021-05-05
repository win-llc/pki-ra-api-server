package com.winllc.pki.ra.beans;

import com.winllc.acme.common.CertSearchParam;
import com.winllc.acme.common.CertSearchParams;
import com.winllc.acme.common.ca.CertAuthority;
import com.winllc.pki.ra.service.transaction.SystemActionRunner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.ldap.LdapName;
import java.math.BigInteger;
import java.security.cert.X509Certificate;

//todo flesh this out, cert interaction should be at object level
public class SystemCertificate {

    private static final Logger log = LogManager.getLogger(SystemCertificate.class);

    private final Name dn;
    private final X509Certificate x509Certificate;
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

    public boolean revoke(int reason) throws Exception {
        try {
            return certAuthority.revokeCertificate(x509Certificate.getSerialNumber().toString(), reason);
        }catch (Exception e){
            log.error("Could not revoke cert: "+dn.toString(), e);
        }
        return false;
    }

    public Name getDn() {
        return dn;
    }

    public BigInteger getSerialNumber(){
        return this.x509Certificate.getSerialNumber();
    }

    public String getIssuerDn(){
        return this.x509Certificate.getIssuerDN().getName();
    }

    public X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    private void sync() throws Exception {
        CertSearchParam param = CertSearchParam.createNew()
                .field(CertSearchParams.CertField.SERIAL)
                .value(x509Certificate.getSerialNumber().toString())
                .relation(CertSearchParams.CertSearchParamRelation.EQUALS);

        X509Certificate cert = certAuthority.getCertificateBySerial(x509Certificate.getSerialNumber().toString());

    }
}
