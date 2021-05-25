package com.winllc.pki.ra.beans.info;

public class CertAuthorityInfo {

    private String dn;
    private String name;
    private String validFrom;
    private String validTo;
    private String trustChain;
    private String latestCrl;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(String validFrom) {
        this.validFrom = validFrom;
    }

    public String getValidTo() {
        return validTo;
    }

    public void setValidTo(String validTo) {
        this.validTo = validTo;
    }

    public String getTrustChain() {
        return trustChain;
    }

    public void setTrustChain(String trustChain) {
        this.trustChain = trustChain;
    }

    public String getLatestCrl() {
        return latestCrl;
    }

    public void setLatestCrl(String latestCrl) {
        this.latestCrl = latestCrl;
    }
}
