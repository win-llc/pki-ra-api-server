package com.winllc.pki.ra.domain;

import com.winllc.acme.common.domain.BaseEntity;
import net.minidev.json.annotate.JsonIgnore;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "cached_certificate"
//        ,
//        uniqueConstraints={
//        @UniqueConstraint(columnNames = {"issuer", "serial"})
//}
)
public class CachedCertificate extends BaseEntity implements Comparable<CachedCertificate> {

    @Column(nullable = false)
    private String dn;
    @Column(name = "issuer", nullable = false)
    private String issuer;
    private String caName;
    @Column(name = "serial", nullable = false)
    private Long serial;
    @Column(nullable = false)
    private Timestamp validFrom;
    @Column(nullable = false)
    private Timestamp validTo;
    private String signatureAlgorithm;
    private Boolean latestForDn;
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name="account_fk")
    private Account account;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getCaName() {
        return caName;
    }

    public void setCaName(String caName) {
        this.caName = caName;
    }

    public Long getSerial() {
        return serial;
    }

    public void setSerial(Long serial) {
        this.serial = serial;
    }

    public Timestamp getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Timestamp validFrom) {
        this.validFrom = validFrom;
    }

    public Timestamp getValidTo() {
        return validTo;
    }

    public void setValidTo(Timestamp validTo) {
        this.validTo = validTo;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Boolean getLatestForDn() {
        return latestForDn;
    }

    public void setLatestForDn(Boolean latestForDn) {
        this.latestForDn = latestForDn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CachedCertificate)) return false;
        //if (!super.equals(o)) return false;
        CachedCertificate that = (CachedCertificate) o;
        return getIssuer().equals(that.getIssuer()) && getSerial().equals(that.getSerial());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getIssuer(), getSerial());
    }

    @Override
    public int compareTo(CachedCertificate o) {
        return o.getValidFrom().compareTo(getValidFrom());
    }

    @Override
    public String toString() {
        return "CachedCertificate{" +
                "dn='" + dn + '\'' +
                ", issuer='" + issuer + '\'' +
                ", serial=" + serial +
                ", validFrom=" + validFrom +
                ", validTo=" + validTo +
                ", signatureAlgorithm='" + signatureAlgorithm + '\'' +
                "} " + super.toString();
    }
}
