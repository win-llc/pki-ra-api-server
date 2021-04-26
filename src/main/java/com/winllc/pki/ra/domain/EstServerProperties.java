package com.winllc.pki.ra.domain;

import com.winllc.acme.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "est_server_properties")
public class EstServerProperties extends BaseEntity {
    @Column(unique = true, nullable = false)
    private String name;
    private String caConnectionName;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCaConnectionName() {
        return caConnectionName;
    }

    public void setCaConnectionName(String caConnectionName) {
        this.caConnectionName = caConnectionName;
    }
}
