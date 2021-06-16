package com.winllc.pki.ra.domain;

import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;

@Entity
@Table(name = "revocation_request",
        uniqueConstraints={
                @UniqueConstraint(columnNames = {"issuerDn", "serial"})
        })
public class RevocationRequest extends RequestEntity {
    private String subjectDn;
    @Column(nullable = false)
    private String issuerDn;
    @Column(nullable = false)
    private String serial;

    public RevocationRequest(String requestedBy) {
        super(requestedBy);
    }

    public RevocationRequest() {
    }

    @PrePersist
    private void prePersist(){
        if(StringUtils.isNotBlank(subjectDn)){
            subjectDn = subjectDn.replace(", ", ",");
        }

        if(StringUtils.isNotBlank(issuerDn)){
            issuerDn = issuerDn.replace(", ", ",");
        }
    }


    public String getSubjectDn() {
        return subjectDn;
    }

    public void setSubjectDn(String subjectDn) {
        this.subjectDn = subjectDn;
    }

    public String getIssuerDn() {
        return issuerDn;
    }

    public void setIssuerDn(String issuerDn) {
        this.issuerDn = issuerDn;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    @Override
    public String toString() {
        return "RevocationRequest{" +
                "subjectDn='" + subjectDn + '\'' +
                ", issuerDn='" + issuerDn + '\'' +
                ", serial='" + serial + '\'' +
                "} " + super.toString();
    }
}
