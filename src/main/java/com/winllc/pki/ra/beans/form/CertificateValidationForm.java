package com.winllc.pki.ra.beans.form;

public class CertificateValidationForm {
    private String serial;
    private String issuerDn;

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getIssuerDn() {
        return issuerDn;
    }

    public void setIssuerDn(String issuerDn) {
        this.issuerDn = issuerDn;
    }
}
