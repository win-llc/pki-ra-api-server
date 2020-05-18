package com.winllc.pki.ra.domain;

import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class CertificateRequest extends AbstractPersistable<Long> implements AccountOwnedEntity {

    @Column(length = 2000)
    private String csr;
    private Timestamp submittedOn;
    private Timestamp reviewedOn;
    private String certAuthorityName;
    private String status;
    @Column(length = 2000)
    private String issuedCertificate;
    @ManyToOne
    @JoinColumn(name="requestedBy_fk")
    private User requestedBy;
    @ManyToOne
    @JoinColumn(name="adminReviewer_fk")
    private User adminReviewer;
    @ElementCollection
    private List<String> requestedDnsNames;
    @ManyToOne
    @JoinColumn(name="account_fk")
    private Account account;
    @ManyToOne
    @JoinColumn(name="serverEntry_fk")
    private ServerEntry serverEntry;

    public static CertificateRequest build(){
        CertificateRequest request = new CertificateRequest();
        request.setSubmittedOn(Timestamp.valueOf(LocalDateTime.now()));
        request.setStatus("new");
        return request;
    }

    public String getCsr() {
        return csr;
    }

    public void setCsr(String csr) {
        this.csr = csr;
    }

    public Timestamp getSubmittedOn() {
        return submittedOn;
    }

    public void setSubmittedOn(Timestamp submittedOn) {
        this.submittedOn = submittedOn;
    }

    public String getCertAuthorityName() {
        return certAuthorityName;
    }

    public void setCertAuthorityName(String certAuthorityName) {
        this.certAuthorityName = certAuthorityName;
    }

    public Timestamp getReviewedOn() {
        return reviewedOn;
    }

    public void setReviewedOn(Timestamp reviewedOn) {
        this.reviewedOn = reviewedOn;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIssuedCertificate() {
        return issuedCertificate;
    }

    public void setIssuedCertificate(String issuedCertificate) {
        this.issuedCertificate = issuedCertificate;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(User requestedBy) {
        this.requestedBy = requestedBy;
    }

    public User getAdminReviewer() {
        return adminReviewer;
    }

    public void setAdminReviewer(User adminReviewer) {
        this.adminReviewer = adminReviewer;
    }

    public List<String> getRequestedDnsNames() {
        return requestedDnsNames;
    }

    public void setRequestedDnsNames(List<String> requestedDnsNames) {
        this.requestedDnsNames = requestedDnsNames;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public ServerEntry getServerEntry() {
        return serverEntry;
    }

    public void setServerEntry(ServerEntry serverEntry) {
        this.serverEntry = serverEntry;
    }
}
