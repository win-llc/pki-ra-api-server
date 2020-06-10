package com.winllc.pki.ra.ca;

import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.domain.CertAuthorityConnectionInfo;

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
    public Certificate[] getTrustChain() throws Exception {
        //todo iterate

        //todo this should be pulled from the connection properties

        String trustChain = getInfo().getTrustChainBase64();

        X509Certificate rootCa = CertUtil.base64ToCert(trustChain);

        return new Certificate[]{rootCa};
    }

    @Override
    public String getName() {
        return name;
    }
}
