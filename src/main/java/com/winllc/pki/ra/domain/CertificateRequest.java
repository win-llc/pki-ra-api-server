package com.winllc.pki.ra.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nimbusds.jose.util.Base64;
import com.winllc.acme.common.util.CertUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "certificate_request",
        uniqueConstraints={
                @UniqueConstraint(columnNames = {"certAuthorityName", "issuedCertificateSerial"})
        })
public class CertificateRequest extends AbstractPersistable<Long> implements AccountOwnedEntity {

    @Column(length = 2000)
    private String csr;
    private Timestamp submittedOn;
    private Timestamp reviewedOn;
    private String certAuthorityName;
    private String status;
    @Column(length = 2000)
    private String issuedCertificate;
    private String issuedCertificateSerial;
    @Column(length = 1000)
    private String publicKeyBase64;
    private String requestedBy;
    private String adminReviewer;
    private String requestedDnsNames;
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

    @PreRemove
    private void preRemove(){
        if(account != null){
            account.getCertificateRequests().remove(this);
        }

        if(serverEntry != null){
            serverEntry.getCertificateRequests().remove(this);
        }
    }

    @JsonIgnore
    public void addPublicKey(PublicKey publicKey){
        this.publicKeyBase64 = Base64.encode(publicKey.getEncoded()).toString();
    }

    @JsonIgnore
    public void addIssuedCertificate(X509Certificate certificate) throws CertificateEncodingException {
        this.issuedCertificate = CertUtil.formatCrtFileContents(certificate);
        addPublicKey(certificate.getPublicKey());
        setIssuedCertificateSerial(certificate.getSerialNumber().toString());
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

    public String getIssuedCertificateSerial() {
        return issuedCertificateSerial;
    }

    public void setIssuedCertificateSerial(String issuedCertificateSerial) {
        this.issuedCertificateSerial = issuedCertificateSerial;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getAdminReviewer() {
        return adminReviewer;
    }

    public void setAdminReviewer(String adminReviewer) {
        this.adminReviewer = adminReviewer;
    }

    public Set<String> getRequestedDnsNamesAsSet(){
        if(StringUtils.isNotBlank(requestedDnsNames)){
            return Stream.of(requestedDnsNames.split(",")).collect(Collectors.toSet());
        }else{
            return new HashSet<>();
        }
    }

    public void addRequestedDnsName(String name){
        Set<String> temp = getRequestedDnsNamesAsSet();
        temp.add(name);
        setRequestedDnsNames(String.join(",", temp));
    }

    public void removeRequestedDnsName(String name){
        Set<String> temp = getRequestedDnsNamesAsSet();
        temp.remove(name);
        setRequestedDnsNames(String.join(",", temp));
    }

    public String getRequestedDnsNames() {
        return requestedDnsNames;
    }

    public void setRequestedDnsNames(String requestedDnsNames) {
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

    public String getPublicKeyBase64() {
        return publicKeyBase64;
    }

    public void setPublicKeyBase64(String publicKeyBase64) {
        this.publicKeyBase64 = publicKeyBase64;
    }


}
