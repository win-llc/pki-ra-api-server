package com.winllc.pki.ra.beans;

import com.winllc.acme.common.CertSearchParam;
import com.winllc.acme.common.CertSearchParams;
import com.winllc.pki.ra.ca.CertAuthority;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

//todo flesh this out, cert interaction should be at object level
public class SystemCertificate {

    private X509Certificate x509Certificate;
    private CertAuthority certAuthority;


    private void sync(){
        CertSearchParam searchParam = new CertSearchParam(CertSearchParams.CertField.SERIAL, x509Certificate.getSerialNumber().toString(),
                CertSearchParams.CertSearchParamRelation.EQUALS);
        //certAuthority.search();
    }
}
