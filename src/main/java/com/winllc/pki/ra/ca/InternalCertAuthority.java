package com.winllc.pki.ra.ca;


import com.winllc.acme.common.CertSearchParam;
import com.winllc.acme.common.CertificateDetails;
import com.winllc.acme.common.SqlCertSearchConverter;
import com.winllc.acme.common.SubjectAltNames;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.domain.CertAuthorityConnectionInfo;
import com.winllc.pki.ra.domain.IssuedCertificate;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class InternalCertAuthority extends AbstractCertAuthority {

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

    public InternalCertAuthority(CertAuthorityConnectionInfo connectionInfo){
        super(connectionInfo);
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

    @Override
    public String getCertificateStatus(String serial) {
        X509Certificate certificateBySerial = getCertificateBySerial(serial);
        if(certificateBySerial != null){
            if(isCertificateRevoked(serial)){
                return "revoked";
            }else{
                return "valid";
            }
        }

        return "unknown";
    }

    @Override
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

    @Override
    public List<String> getRequiredConnectionProperties() {
        return requiredProperties;
    }

    @Override
    public Map<String, String> getDefaultProperties() {
        return defaultProperties;
    }

    public X509Certificate issueCertificate(String pkcs10, SubjectAltNames sans) {

        try {
            PKCS10CertificationRequest certificationRequest = CertUtil.csrBase64ToPKC10Object(pkcs10);
            KeyStore ks = loadKeystore(caKeystoreLocation, caKeystorePassword);
            X509Certificate certificate = signCSR(certificationRequest, sans, 30, ks, caKeystoreAlias, caKeystorePassword.toCharArray());

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

    private X509Certificate signCSR(PKCS10CertificationRequest csr, SubjectAltNames subjectAltNames, int validity,
                                    KeyStore keystore, String alias, char[] password) throws Exception {
        try {
            Security.addProvider(new BouncyCastleProvider());

            PrivateKey cakey = (PrivateKey) keystore.getKey(alias, password);
            Certificate[] chain = keystore.getCertificateChain(alias);
            Certificate root = chain[1];
            X509Certificate intermediate = (X509Certificate) chain[0];

            AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256withRSA");
            AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
            X500Name issuer = new JcaX509CertificateHolder(intermediate).getSubject();
            BigInteger serial = new BigInteger(16, new SecureRandom());
            Date from = new Date();
            Date to = new Date(System.currentTimeMillis() + (validity * 86400000L));


            X509v3CertificateBuilder certgen = new X509v3CertificateBuilder(issuer, serial, from, to,
                    new X500Name("cn=" + "test"), csr.getSubjectPublicKeyInfo());
            certgen.addExtension(X509Extension.basicConstraints, false, new BasicConstraints(false));
            //certgen.addExtension(X509Extension.subjectKeyIdentifier, false, new SubjectKeyIdentifier(csr.getSubjectPublicKeyInfo()));

            certgen.addExtension(X509Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifierStructure(intermediate.getPublicKey()));
            List<Integer> keyUsages = new ArrayList<Integer>();

            keyUsages.add(KeyUsage.digitalSignature);

            if (keyUsages.size() == 0) {
                keyUsages.add(KeyUsage.digitalSignature);
            }

            Iterator<Integer> usageIterable = keyUsages.iterator();
            int usageBit = 0;
            while (usageIterable.hasNext()) {
                Integer usage = usageIterable.next();
                usageBit = usageBit | usage;
            }

            certgen.addExtension(X509Extension.keyUsage, false, new KeyUsage(usageBit));
            List<KeyPurposeId> keyPurposeIds = new ArrayList<>();
            keyPurposeIds.add(KeyPurposeId.id_kp_clientAuth);
            keyPurposeIds.add(KeyPurposeId.id_kp_serverAuth);

            if (keyPurposeIds.size() > 0) {
                org.bouncycastle.asn1.x509.ExtendedKeyUsage eku =
                        new org.bouncycastle.asn1.x509.ExtendedKeyUsage(keyPurposeIds.toArray(new KeyPurposeId[0]));
                certgen.addExtension(X509Extension.extendedKeyUsage, false, eku);
            }

            if(subjectAltNames != null) {
                List<GeneralName> generalNameList = new ArrayList<>();
                for (String dns : subjectAltNames.getValuesForType(SubjectAltNames.SubjAltNameType.DNS)) {
                    GeneralName altName = new GeneralName(GeneralName.dNSName, dns);
                    generalNameList.add(altName);
                }

                GeneralNames subjectAltName = new GeneralNames(generalNameList.toArray(new GeneralName[0]));
                certgen.addExtension(X509Extensions.SubjectAlternativeName, false, subjectAltName);
            }

            AsymmetricKeyParameter foo = PrivateKeyFactory.createKey(cakey.getEncoded());
            ContentSigner signer = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(foo);

            X509CertificateHolder holder = certgen.build(signer);
            byte[] certencoded = holder.toASN1Structure().getEncoded();

            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
            signer = new JcaContentSignerBuilder("SHA256withRSA").build(cakey);
            generator.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build()).build(signer, intermediate));
            generator.addCertificate(new X509CertificateHolder(certencoded));
            generator.addCertificate(new X509CertificateHolder(intermediate.getEncoded()));
            generator.addCertificate(new X509CertificateHolder(root.getEncoded()));
            CMSTypedData content = new CMSProcessableByteArray(certencoded);
            CMSSignedData signeddata = generator.generate(content, true);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write("-----BEGIN PKCS #7 SIGNED DATA-----\n".getBytes("ISO-8859-1"));
            out.write(org.bouncycastle.util.encoders.Base64.encode(signeddata.getEncoded()));
            out.write("\n-----END PKCS #7 SIGNED DATA-----\n".getBytes("ISO-8859-1"));
            out.close();
            //return new String(out.toByteArray(), "ISO-8859-1");
            X509CertificateHolder x509CertificateHolder = new X509CertificateHolder(certencoded);
            X509Certificate issuedCert = new JcaX509CertificateConverter().setProvider("BC")
                    .getCertificate(x509CertificateHolder);
            //internal store only
            issuedCerts.add(issuedCert);
            return issuedCert;
        }catch (Exception e){
            e.printStackTrace();
            throw e;
        }
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
        IssuedCertificate issuedCertificate = new IssuedCertificate();
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
