package com.winllc.pki.ra.ca;

import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.domain.CertAuthorityConnectionInfo;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.springframework.ldap.support.LdapNameBuilder;

import javax.naming.Name;
import javax.naming.ldap.LdapName;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public abstract class AbstractCertAuthority implements CertAuthority {

    protected CertAuthorityConnectionInfo info;

    protected CertAuthorityConnectionType type;
    protected String name;

    protected AbstractCertAuthority(CertAuthorityConnectionInfo info) {
        this.info = info;
        this.name = info.getName();
        this.type = info.getType();
    }

    @Override
    public CertAuthorityConnectionType getType() {
        return type;
    }

    protected CertAuthorityConnectionInfo getInfo(){
        return this.info;
    }

    @Override
    public CertAuthorityConnectionInfo getConnectionInfo() {
        return info;
    }

    @Override
    public Certificate[] getTrustChain() throws Exception {
        String trustChain = getInfo().getTrustChainBase64();

        return CertUtil.trustChainStringToCertArray(trustChain);
    }

    @Override
    public Name getIssuerName() throws Exception {
        Certificate[] chain = getTrustChain();
        if(chain != null && chain.length > 0){
            Certificate issuerCert = chain[0];
            if(issuerCert instanceof X509Certificate){
                Principal principal = ((X509Certificate) issuerCert).getSubjectDN();
                return new LdapName(principal.getName());
            }else{
                throw new RuntimeException("Unexpected cert type: "+issuerCert.getClass().getCanonicalName());
            }
        }else{
            throw new RuntimeException("Can't get Issuer name, no chain found");
        }
    }

    @Override
    public String getName() {
        return name;
    }
}
