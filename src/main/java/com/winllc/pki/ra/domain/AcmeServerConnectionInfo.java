package com.winllc.pki.ra.domain;

import javax.persistence.Entity;

@Entity
public class AcmeServerConnectionInfo extends BaseEntity {

    private String name;
    private String url;

    public AcmeServerConnectionInfo(String name, String url) {
        this.url = url;
        this.name = name;
    }

    private AcmeServerConnectionInfo() {
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}
