package com.winllc.pki.ra.domain;

import javax.persistence.Entity;

@Entity
public class InternalAccount extends BaseEntity {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
