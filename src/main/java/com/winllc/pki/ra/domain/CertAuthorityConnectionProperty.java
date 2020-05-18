package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class CertAuthorityConnectionProperty extends AbstractPersistable<Long> {

    private String name;
    private String value;
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name="certAuthorityConnectionInfo_fk")
    private CertAuthorityConnectionInfo certAuthorityConnectionInfo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public CertAuthorityConnectionInfo getCertAuthorityConnectionInfo() {
        return certAuthorityConnectionInfo;
    }

    public void setCertAuthorityConnectionInfo(CertAuthorityConnectionInfo certAuthorityConnectionInfo) {
        this.certAuthorityConnectionInfo = certAuthorityConnectionInfo;
    }
}
