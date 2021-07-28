package com.winllc.pki.ra.ca;


import com.winllc.acme.common.CertSearchParam;
import com.winllc.acme.common.CertificateDetails;
import com.winllc.acme.common.SqlCertSearchConverter;
import com.winllc.acme.common.SubjectAltNames;
import com.winllc.acme.common.ca.ConnectionProperty;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.acme.common.constants.CertificateStatus;
import com.winllc.acme.common.domain.CertAuthorityConnectionInfo;
import com.winllc.acme.common.domain.IssuedCertificate;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class InternalCertAuthority {

    private static final List<String> requiredProperties;
    private static final Map<String, String> defaultProperties;

    static{
        requiredProperties = new ArrayList<>();
        requiredProperties.add("BASE_URL");

        defaultProperties = new HashMap<>();
    }

    private String name;
    private String caKeystorePassword = "P@ssW0rd";
    private String caKeystoreLocation = "C:\\Users\\jrmints\\IdeaProjects\\PKI Registration Authority\\src\\main\\resources\\ca-internal\\win-llc-intermediate-2.pfx";
    private String caKeystoreAlias = "alias";

    private EntityManagerFactory entityManager;

    private List<X509Certificate> issuedCerts = new ArrayList<>();
    private List<X509Certificate> revokedCerts = new ArrayList<>();

    public InternalCertAuthority(CertAuthorityConnectionInfo connectionInfo, EntityManagerFactory entityManager){
        //super(connectionInfo);
        this.entityManager = entityManager;
    }

    public boolean revokeCertificate(String serial, int reason) {
        //if(listContainsCert(issuedCerts, certificate) && !listContainsCert(revokedCerts, certificate)){
        Optional<X509Certificate> optionalX509Certificate = getX509CertBySerial(serial);
        if(optionalX509Certificate.isPresent()){
            X509Certificate cert = optionalX509Certificate.get();
            revokedCerts.add(cert);
            return true;
        }

        return false;
    }

    public CertificateStatus getCertificateStatus(String serial) throws Exception {
        X509Certificate certificateBySerial = getCertificateBySerial(serial);
        if(certificateBySerial != null){
            if(isCertificateRevoked(serial)){
                return CertificateStatus.REVOKED;
            }else{
                return CertificateStatus.VALID;
            }
        }

        throw new Exception("Could not determine cert status");
    }

    public List<CertificateDetails> search(CertSearchParam param) {
        //todo
        String query = param.buildQuery(SqlCertSearchConverter.build());

        query = "select * from issued_certificate where "+query;

        EntityManager em = entityManager.createEntityManager();
        Query nativeQuery = em.createNativeQuery(query, IssuedCertificate.class);
        List<IssuedCertificate> resultList = nativeQuery.getResultList();

        return resultList.stream()
                .map(r -> r.convertToCertDetails())
                .collect(Collectors.toList());
    }

    public List<ConnectionProperty> getRequiredProperties() {
        return new ArrayList<>();
    }

    public X509Certificate issueCertificate(String pkcs10, String dn, SubjectAltNames sans) {

        try {
            PKCS10CertificationRequest certificationRequest = CertUtil.csrBase64ToPKC10Object(pkcs10);
            KeyStore ks = loadKeystore(caKeystoreLocation, caKeystorePassword);
            X509Certificate certificate = CertUtil.signCSR(certificationRequest, sans, 30, ks,
                    caKeystoreAlias, caKeystorePassword.toCharArray());

            issuedCerts.add(certificate);


            IssuedCertificate issuedCertificate = x509ToIssuedCertificate(certificate, "VALID");
            EntityManager em = entityManager.createEntityManager();
            em.getTransaction().begin();
            em.persist(issuedCertificate);
            em.getTransaction().commit();

            return certificate;
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    public Certificate[] getTrustChain() {
        try {
            KeyStore keyStore = loadKeystore(caKeystoreLocation, caKeystorePassword);
            Certificate[] chain = keyStore.getCertificateChain(caKeystoreAlias);
            return chain;
        }catch (Exception e){
            e.printStackTrace();
        }

        return new Certificate[0];
    }

    public X509Certificate getCertificateBySerial(String serial){

        Optional<X509Certificate> optionalCert = getX509CertBySerial(serial);

        if(optionalCert.isPresent()){
            try {
                return optionalCert.get();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        return null;
    }

    public boolean isCertificateRevoked(String serial) {

        for(X509Certificate cert : revokedCerts){
            if(cert.getSerialNumber() == BigInteger.valueOf(Long.valueOf(serial))){
                return true;
            }
        }

        return false;
    }

    private Optional<X509Certificate> getX509CertBySerial(String serial){
        for(X509Certificate cert : issuedCerts){
            BigInteger certSerial = cert.getSerialNumber();
            BigInteger reqSerial = BigInteger.valueOf(Long.valueOf(serial));
            if(certSerial.equals(reqSerial)){
                try {
                    return Optional.of(cert);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return Optional.empty();
    }


    private KeyStore loadKeystore(String location, String password) throws Exception {
        System.out.println("Loading keystore: "+location);
        FileInputStream fis = new FileInputStream(location);

        KeyStore ks = KeyStore.getInstance("PKCS12");

        ks.load(fis, password.toCharArray());
        IOUtils.closeQuietly(fis);
        return ks;
    }

    private IssuedCertificate x509ToIssuedCertificate(X509Certificate x509Certificate, String status) throws CertificateEncodingException {
        IssuedCertificate issuedCertificate = IssuedCertificate.buildNew();
        issuedCertificate.setSubjectDn(x509Certificate.getSubjectDN().getName());
        issuedCertificate.setIssuerDn(x509Certificate.getIssuerDN().getName());
        issuedCertificate.setIssuedOn(Timestamp.from(x509Certificate.getNotBefore().toInstant()));
        issuedCertificate.setExpiresOn(Timestamp.from(x509Certificate.getNotAfter().toInstant()));
        issuedCertificate.setIssuedCertificate(CertUtil.formatCrtFileContents(x509Certificate));
        issuedCertificate.setStatus(status);
        return issuedCertificate;
    }

    public void setEntityManager(EntityManagerFactory entityManager) {
        this.entityManager = entityManager;
    }
}
