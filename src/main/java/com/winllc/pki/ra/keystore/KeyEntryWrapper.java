package com.winllc.pki.ra.keystore;

import java.security.Key;
import java.security.cert.Certificate;

public class KeyEntryWrapper {
    private String alias;
    private Key key;
    private Certificate certificate;
    private Certificate[] chain;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    public Certificate[] getChain() {
        return chain;
    }

    public void setChain(Certificate[] chain) {
        this.chain = chain;
    }
}
