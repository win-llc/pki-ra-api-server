package com.winllc.pki.ra.service.transaction;

import com.winllc.ra.integration.ca.CertAuthority;
import org.springframework.context.ApplicationContext;

public abstract class CertTransaction {

    protected final CertAuthority certAuthority;
    protected final ApplicationContext context;

    protected CertTransaction(CertAuthority certAuthority, ApplicationContext context){
        this.certAuthority = certAuthority;
        this.context = context;
    }
}
