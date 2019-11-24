package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.List;

@Entity
public class CertAuthorityType extends BaseEntity {

    private String name;
    @ElementCollection
    private List<String> requiredSettings;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getRequiredSettings() {
        return requiredSettings;
    }

    public void setRequiredSettings(List<String> requiredSettings) {
        this.requiredSettings = requiredSettings;
    }
}