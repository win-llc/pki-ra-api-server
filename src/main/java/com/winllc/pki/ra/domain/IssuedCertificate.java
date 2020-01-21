package com.winllc.pki.ra.domain;

import com.winllc.acme.common.CertificateDetails;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "IssuedCertificate")
public class IssuedCertificate extends AbstractPersistable<Long> {

    private String certAuthorityName;
    private String issuerDn;
    private String subjectDn;
    @Column(length = 2000)
    private String issuedCertificate;
    private Timestamp issuedOn;
    private Timestamp revokedOn;
    private Timestamp expiresOn;
    private String status;
    private String serial;

    public CertificateDetails convertToCertDetails(){
        CertificateDetails details = new CertificateDetails();
        details.setStatus(this.getStatus());
        details.setIssuer(this.getIssuerDn());
        details.setCertificateBase64(this.getIssuedCertificate());

        return details;
    }

    public String getCertAuthorityName() {
        return certAuthorityName;
    }

    public void setCertAuthorityName(String certAuthorityName) {
        this.certAuthorityName = certAuthorityName;
    }

    public String getIssuerDn() {
        return issuerDn;
    }

    public void setIssuerDn(String issuerDn) {
        this.issuerDn = issuerDn;
    }

    public String getSubjectDn() {
        return subjectDn;
    }

    public void setSubjectDn(String subjectDn) {
        this.subjectDn = subjectDn;
    }

    public String getIssuedCertificate() {
        return issuedCertificate;
    }

    public void setIssuedCertificate(String issuedCertificate) {
        this.issuedCertificate = issuedCertificate;
    }

    public Timestamp getIssuedOn() {
        return issuedOn;
    }

    public void setIssuedOn(Timestamp issuedOn) {
        this.issuedOn = issuedOn;
    }

    public Timestamp getRevokedOn() {
        return revokedOn;
    }

    public void setRevokedOn(Timestamp revokedOn) {
        this.revokedOn = revokedOn;
    }

    public Timestamp getExpiresOn() {
        return expiresOn;
    }

    public void setExpiresOn(Timestamp expiresOn) {
        this.expiresOn = expiresOn;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }
}
