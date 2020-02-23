package com.winllc.pki.ra.ca;

import com.winllc.pki.ra.domain.CertAuthorityConnectionInfo;

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

    @Override
    public String getName() {
        return name;
    }
}
