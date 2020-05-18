package com.winllc.pki.ra.domain;

import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class ServerSettings extends AbstractPersistable<Long> {

    @Column(unique = true)
    private String property;
    private String value = "";

    public ServerSettings(){}

    public ServerSettings(String property){
        this.property = property;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
