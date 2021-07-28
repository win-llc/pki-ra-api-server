package com.winllc.pki.ra.beans.info;

import com.winllc.acme.common.domain.CertificateRequest;

public class CertificateRequestInfo extends InfoObject<CertificateRequest> {

    private String csr;
    private AccountInfo accountInfo;

    public CertificateRequestInfo(CertificateRequest certificateRequest){
        super(certificateRequest);

        this.csr = certificateRequest.getCsr();

        if(certificateRequest.getAccount() != null){
            this.accountInfo = new AccountInfo(certificateRequest.getAccount(), false);
        }
    }

    public String getCsr() {
        return csr;
    }

    public void setCsr(String csr) {
        this.csr = csr;
    }

    public AccountInfo getAccountInfo() {
        return accountInfo;
    }

    public void setAccountInfo(AccountInfo accountInfo) {
        this.accountInfo = accountInfo;
    }
}
