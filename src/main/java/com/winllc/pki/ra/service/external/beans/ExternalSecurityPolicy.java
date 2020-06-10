package com.winllc.pki.ra.service.external.beans;

public class ExternalSecurityPolicy {
    //todo maps to a policy service or directory
    //use external policies to help decide attribute policies

    private String source;
    private String attribute;
    private String value;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
