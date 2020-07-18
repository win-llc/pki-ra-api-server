package com.winllc.pki.ra.domain;

import com.winllc.acme.common.CertificateDetails;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name = "issued_certificate")
public class IssuedCertificate extends UniqueEntity implements AccountOwnedEntity {

    private String certAuthorityName;
    @Column(nullable = false)
    private String issuerDn;
    @Column(nullable = false)
    private String subjectDn;
    @Column(length = 2000)
    private String issuedCertificate;
    private Timestamp issuedOn;
    private Timestamp revokedOn;
    private Timestamp expiresOn;
    private String status;
    @Column(nullable = false)
    private String serial;

    @ManyToOne
    @JoinColumn(name="account_fk")
    private Account account;

    private IssuedCertificate(){}

    public static IssuedCertificate buildNew(){
        IssuedCertificate issuedCertificate = new IssuedCertificate();
        issuedCertificate.setUuid(UUID.randomUUID());
        return issuedCertificate;
    }

    public CertificateDetails convertToCertDetails(){
        CertificateDetails details = new CertificateDetails();
        details.setStatus(this.getStatus());
        details.setIssuer(this.getIssuerDn());
        details.setCertificateBase64(this.getIssuedCertificate());
        return details;
    }

    @PreRemove
    private void preRemove(){
        if(account != null){
            account.getIssuedCertificates().remove(this);
        }
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

    @Override
    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
