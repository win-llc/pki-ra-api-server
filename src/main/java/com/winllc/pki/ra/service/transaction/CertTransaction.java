package com.winllc.pki.ra.service.transaction;

import com.winllc.acme.common.ca.CertAuthority;

public abstract class CertTransaction {

    protected CertAuthority certAuthority;

    protected CertTransaction(CertAuthority certAuthority){
        this.certAuthority = certAuthority;
    }
}
