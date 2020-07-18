package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.keystore.KeyEntryWrapper;

import java.security.cert.Certificate;

public class AppKeyStoreEntryForm {

    private String alias;
    private boolean generateCsr = false;
    private String currentCertificate;
    private String uploadCertificate;
    private String uploadChain;
    private String currentCertDetails;

    public AppKeyStoreEntryForm(){}

    public AppKeyStoreEntryForm(KeyEntryWrapper entryWrapper){
        this.alias = entryWrapper.getAlias();
        Certificate cert = entryWrapper.getCertificate();
        if(cert != null){
            this.currentCertificate = CertUtil.toPEM(cert);
        }
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isGenerateCsr() {
        return generateCsr;
    }

    public void setGenerateCsr(boolean generateCsr) {
        this.generateCsr = generateCsr;
    }

    public String getCurrentCertificate() {
        return currentCertificate;
    }

    public void setCurrentCertificate(String currentCertificate) {
        this.currentCertificate = currentCertificate;
    }

    public String getUploadCertificate() {
        return uploadCertificate;
    }

    public void setUploadCertificate(String uploadCertificate) {
        this.uploadCertificate = uploadCertificate;
    }

    public String getUploadChain() {
        return uploadChain;
    }

    public void setUploadChain(String uploadChain) {
        this.uploadChain = uploadChain;
    }

    public String getCurrentCertDetails() {
        return currentCertDetails;
    }

    public void setCurrentCertDetails(String currentCertDetails) {
        this.currentCertDetails = currentCertDetails;
    }
}
