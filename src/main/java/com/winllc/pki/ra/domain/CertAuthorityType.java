package com.winllc.pki.ra.domain;

import com.winllc.acme.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.List;

@Entity
@Table(name = "cert_authority_type")
public class CertAuthorityType extends BaseEntity {

    @Column(unique = true)
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
