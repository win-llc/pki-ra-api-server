package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.client.ca.CachedCertificate;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@Setter
public class CertificateForm extends ValidForm {

    private String dn;
    private String issuer;
    private String caName;
    private String serial;
    private Date validFrom;
    private Date validTo;
    private String status;

    public CertificateForm(CachedCertificate certificate){
        setId(ThreadLocalRandom.current().nextLong());
        setDn(certificate.getDn());
        setIssuer(certificate.getIssuer());
        setCaName(certificate.getCaName());
        setSerial(certificate.getSerial());
        setValidFrom(certificate.getValidFrom());
        setValidTo(certificate.getValidTo());
        setStatus(certificate.getStatus());
    }

    @Override
    protected void processIsValid() {

    }
}
