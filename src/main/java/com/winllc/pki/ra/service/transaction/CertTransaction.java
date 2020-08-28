package com.winllc.pki.ra.service.transaction;

import com.winllc.pki.ra.ca.CertAuthority;
import org.springframework.context.ApplicationContext;

public abstract class CertTransaction {

    protected CertAuthority certAuthority;

    protected CertTransaction(CertAuthority certAuthority){
        this.certAuthority = certAuthority;
    }
}
