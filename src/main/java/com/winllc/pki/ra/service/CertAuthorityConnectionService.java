package com.winllc.pki.ra.service;

import com.winllc.acme.common.*;
import com.winllc.acme.common.ra.RACertificateIssueRequest;
import com.winllc.acme.common.ra.RACertificateRevokeRequest;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.beans.form.CertAuthorityConnectionInfoForm;
import com.winllc.pki.ra.beans.validator.CertAuthorityConnectionInfoValidator;
import com.winllc.pki.ra.ca.CertAuthority;
import com.winllc.pki.ra.ca.CertAuthorityConnectionType;
import com.winllc.pki.ra.ca.DogTagCertAuthority;
import com.winllc.pki.ra.ca.InternalCertAuthority;
import com.winllc.pki.ra.constants.AuditRecordType;
import com.winllc.pki.ra.domain.*;
import com.winllc.pki.ra.keystore.ApplicationKeystore;
import com.winllc.pki.ra.repository.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.transaction.Transactional;
import javax.validation.Valid;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/ca")
@Transactional
public class CertAuthorityConnectionService {

    private static final Logger log = LogManager.getLogger(CertAuthorityConnectionService.class);

    @Autowired
    private CertAuthorityConnectionInfoRepository repository;
    @Autowired
    private CertAuthorityConnectionPropertyRepository propertyRepository;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private CertAuthorityConnectionInfoValidator validator;
    @Autowired
    private AuditRecordRepository auditRecordRepository;
    @Autowired
    private ApplicationKeystore applicationKeystore;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CertificateRequestRepository certificateRequestRepository;

    private Map<String, CertAuthority> loadedCertAuthorities = new HashMap<>();


    @PostConstruct
    private void init() {
        for (String connectionInfoName : repository.findAllNames()) {
            loadCertAuthority(connectionInfoName);
        }
    }


    //Build CA object for use
    private void loadCertAuthority(String name) {
        Optional<CertAuthority> optionalCertAuthority = buildCertAuthority(name);
        if (optionalCertAuthority.isPresent()) {
            CertAuthority ca = optionalCertAuthority.get();

            if (ca instanceof InternalCertAuthority) {
                ((InternalCertAuthority) ca).setEntityManager(entityManagerFactory);
            }

            loadedCertAuthorities.put(ca.getName(), ca);
        }
    }

    @PostMapping("/api/info/create")
    public ResponseEntity<?> createConnectionInfo(@Valid @RequestBody CertAuthorityConnectionInfoForm connectionInfo) {
        boolean valid = validator.validate(connectionInfo, false);

        if (valid) {
            CertAuthorityConnectionInfo caConnection = new CertAuthorityConnectionInfo();
            caConnection.setName(connectionInfo.getName());
            caConnection.setType(CertAuthorityConnectionType.valueOf(connectionInfo.getType()));
            caConnection = repository.save(caConnection);

            loadCertAuthority(caConnection.getName());

            CertAuthority ca = loadedCertAuthorities.get(caConnection.getName());

            //Create the required settings for the connection, will be filled in on edit screen
            Set<CertAuthorityConnectionProperty> props = new HashSet<>();
            for(String requiredProp : ca.getRequiredConnectionProperties()){
                CertAuthorityConnectionProperty prop = new CertAuthorityConnectionProperty();
                prop.setName(requiredProp);
                prop.setValue("");
                prop.setCertAuthorityConnectionInfo(caConnection);
                prop = propertyRepository.save(prop);
                props.add(prop);
            }

            caConnection.setProperties(props);
            caConnection = repository.save(caConnection);

            //reload cert authority
            loadCertAuthority(caConnection.getName());

            return ResponseEntity.ok(connectionInfo.getId());
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @Transactional
    @PostMapping("/api/info/update")
    public ResponseEntity<?> updateConnectionInfo(@Valid @RequestBody CertAuthorityConnectionInfoForm form) {
        //todo validate

        boolean valid = validator.validate(form, true);

        if(valid) {
            Optional<CertAuthorityConnectionInfo> optionalInfo = repository.findById(form.getId());
            if (optionalInfo.isPresent()) {
                final CertAuthorityConnectionInfo info = optionalInfo.get();

                Set<CertAuthorityConnectionProperty> props = new HashSet<>();
                if(!CollectionUtils.isEmpty(form.getProperties())){
                    for(CertAuthorityConnectionProperty prop : form.getProperties()){
                        prop.setCertAuthorityConnectionInfo(info);
                        prop = propertyRepository.save(prop);
                        props.add(prop);
                    }
                }
                info.setProperties(props);

                CertAuthorityConnectionInfo info2 = repository.save(info);
                return ResponseEntity.ok(buildForm(info2));
            }
        }else{
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/api/info/byName/{name}")
    @Transactional
    public ResponseEntity<?> getConnectionInfoByName(@PathVariable String name) {
        Optional<CertAuthorityConnectionInfo> infoOptional = repository.findByName(name);

        if (infoOptional.isPresent()) {
            return ResponseEntity.ok(buildForm(infoOptional.get()));
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping("/api/info/byId/{id}")
    @Transactional
    public ResponseEntity<?> getConnectionInfoById(@PathVariable Long id) {
        Optional<CertAuthorityConnectionInfo> infoOptional = repository.findById(id);

        if (infoOptional.isPresent()) {
            return ResponseEntity.ok(buildForm(infoOptional.get()));
        }

        return ResponseEntity.noContent().build();
    }

    private CertAuthorityConnectionInfoForm buildForm(CertAuthorityConnectionInfo info){
        Hibernate.initialize(info.getProperties());
        CertAuthority ca = loadedCertAuthorities.get(info.getName());
        CertAuthorityConnectionInfoForm form = new CertAuthorityConnectionInfoForm(info, ca);
        return form;
    }

    @GetMapping("/api/info/all")
    public ResponseEntity<?> getAllConnectionInfo() {
        List<CertAuthorityConnectionInfo> list = repository.findAll();

        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/api/info/delete/{id}")
    public ResponseEntity<?> deleteInfo(@PathVariable Long id) {
        repository.deleteById(id);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/info/getTypes")
    public ResponseEntity<?> getTypes() {
        List<String> certAuthorityTypes = Stream.of(CertAuthorityConnectionType.values())
                .map(v -> v.name())
                .collect(Collectors.toList());

        return ResponseEntity.ok(certAuthorityTypes);
    }


    @PostMapping("/issueCertificate")
    public ResponseEntity<?> issueCertificate(@Valid @RequestBody RACertificateIssueRequest raCertificateIssueRequest) throws Exception {

        Optional<Account> optionalAccount = accountRepository.findByKeyIdentifierEquals(raCertificateIssueRequest.getAccountKid());
        if(optionalAccount.isPresent()) {
            X509Certificate cert = processIssueCertificate(raCertificateIssueRequest);
            String pemCert = CertUtil.convertToPem(cert);

            Account account = optionalAccount.get();

            //add as certificate request
            CertificateRequest certificateRequest = new CertificateRequest();
            certificateRequest.setAccount(account);
            certificateRequest.setCsr(raCertificateIssueRequest.getCsr());
            certificateRequest.setCertAuthorityName(raCertificateIssueRequest.getCertAuthorityName());
            certificateRequest.setStatus("issued");
            certificateRequest.setSubmittedOn(Timestamp.valueOf(LocalDateTime.now()));
            certificateRequest.setIssuedCertificate(pemCert);
            certificateRequestRepository.save(certificateRequest);

            //todo consolidate this somewhere else
            AuditRecord record = AuditRecord.buildNew(AuditRecordType.CERTIFICATE_ISSUED);
            record.setAccountKid(raCertificateIssueRequest.getAccountKid());
            record.setSource("acme");
            auditRecordRepository.save(record);

            return ResponseEntity.ok(pemCert);
        }else{
            log.error("Could not find account with: "+raCertificateIssueRequest.getAccountKid());
            return ResponseEntity.badRequest().build();
        }
    }

    public X509Certificate processIssueCertificate(RACertificateIssueRequest certificateRequest) throws Exception {

        CertAuthority certAuthority = loadedCertAuthorities.get(certificateRequest.getCertAuthorityName());

        SubjectAltNames subjectAltNames = null;
        String dnsNames = certificateRequest.getDnsNames();
        if (StringUtils.isNotBlank(dnsNames)) {
            subjectAltNames = new SubjectAltNames();
            for (String dnsName : dnsNames.split(",")) {
                subjectAltNames.addValue(SubjectAltNames.SubjAltNameType.DNS, dnsName);
            }
        }

        X509Certificate cert = certAuthority.issueCertificate(certificateRequest.getCsr(), subjectAltNames);
        if (cert != null) {
            return cert;
        } else {
            throw new Exception("Could not issue certificate");
        }
    }

    @PostMapping("/revokeCertificate")
    public ResponseEntity<?> revokeCertificate(@Valid @RequestBody RACertificateRevokeRequest revokeRequest) throws Exception {
        CertAuthority certAuthority = loadedCertAuthorities.get(revokeRequest.getCertAuthorityName());
        if (certAuthority != null) {
            String serial = revokeRequest.getSerial();
            //Get serial from certificate request
            if(StringUtils.isBlank(serial)){
                if(revokeRequest.getRequestId() != null){
                    Optional<CertificateRequest> optionalCertificateRequest = certificateRequestRepository.findById(Long.valueOf(revokeRequest.getRequestId()));
                    if(optionalCertificateRequest.isPresent()){
                        CertificateRequest certificateRequest = optionalCertificateRequest.get();
                        if(StringUtils.isNotBlank(certificateRequest.getIssuedCertificate())){
                            X509Certificate x509Certificate = CertUtil.base64ToCert(certificateRequest.getIssuedCertificate());
                            serial = x509Certificate.getSerialNumber().toString();
                        }
                    }
                }
            }

            if(StringUtils.isBlank(serial)) throw new Exception("Request ID and Serial can't both be null");

            boolean revoked = certAuthority.revokeCertificate(serial, revokeRequest.getReason());
            if (revoked) {
                //todo consolidate this somewhere else
                AuditRecord record = AuditRecord.buildNew(AuditRecordType.CERTIFICATE_REVOKED);
                //todo get account KID and add to record
                auditRecordRepository.save(record);
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/certDetails/{connectionName}")
    public ResponseEntity<?> getCertificateStatus(@PathVariable String connectionName, @RequestParam String serial) throws Exception {

        CertAuthority certAuthority = loadedCertAuthorities.get(connectionName);
        if (certAuthority != null) {
            CertificateDetails details = new CertificateDetails();
            X509Certificate cert = certAuthority.getCertificateBySerial(serial);
            if (cert != null) {
                String status = certAuthority.getCertificateStatus(serial);
                try {
                    details.setCertificateBase64(CertUtil.convertToPem(cert));
                    details.setStatus(status);
                    details.setSerial(serial);

                    return ResponseEntity.ok(details);
                } catch (CertificateEncodingException e) {
                    e.printStackTrace();
                    return ResponseEntity.status(500).build();
                }
            }
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/trustChain/{connectionName}")
    public ResponseEntity<?> getTrustChain(@PathVariable String connectionName) {
        CertAuthority certAuthority = loadedCertAuthorities.get(connectionName);

        Certificate[] trustChain = certAuthority.getTrustChain();

        StringBuilder stringBuilder = new StringBuilder();
        for (Certificate cert : trustChain) {
            try {
                stringBuilder.append(CertUtil.convertToPem(cert)).append("\n");
            } catch (CertificateEncodingException e) {
                e.printStackTrace();
            }
        }
        return ResponseEntity.ok(stringBuilder.toString());
    }


    @PostMapping("/search/{connectionName}")
    public ResponseEntity<?> search(@PathVariable String connectionName, @RequestBody CertSearchParam certSearchParam) {
        CertAuthority certAuthority = loadedCertAuthorities.get(connectionName);

        /*
        CertSearchParam param = new CertSearchParam(CertSearchParams.CertSearchParamRelation.AND);

        CertSearchParam sub1 = new CertSearchParam(CertSearchParams.CertField.SUBJECT, "te", CertSearchParams.CertSearchParamRelation.CONTAINS);
        CertSearchParam sub2 = new CertSearchParam(CertSearchParams.CertField.SUBJECT, "st", CertSearchParams.CertSearchParamRelation.CONTAINS);

        param.addSearchParam(sub1);
        param.addSearchParam(sub2);

        CertSearchParam param2 = new CertSearchParam(CertSearchParams.CertSearchParamRelation.OR);
        param2.addSearchParam(param);
        param2.addSearchParam(sub1);

        List<CertificateDetails> temp1 = certAuthority.search(param2);

        List<CertificateDetails> temp2 = certAuthority.search(sub1);
        */
        //todo clean this up

        return ResponseEntity.ok(certAuthority.search(certSearchParam));
    }

    public Optional<CertAuthority> getCertAuthorityByName(String name) {
        CertAuthority ca = loadedCertAuthorities.get(name);

        if (ca != null) {
            return Optional.of(ca);
        } else {
            return Optional.empty();
        }
    }

    public List<CertAuthority> getAllCertAuthorities() {
        return new ArrayList<>(loadedCertAuthorities.values());
    }

    @Transactional
    public Optional<CertAuthority> buildCertAuthority(String connectionName) {
        CertAuthority certAuthority = null;
        Optional<CertAuthorityConnectionInfo> infoOptional = repository.findByName(connectionName);

        if (infoOptional.isPresent()) {
            CertAuthorityConnectionInfo info = infoOptional.get();

            try {
                Hibernate.initialize(info.getProperties());
                switch (info.getType()) {
                    case INTERNAL:
                        certAuthority = new InternalCertAuthority(info);
                        break;
                    case DOGTAG:
                        certAuthority = new DogTagCertAuthority(info, applicationKeystore);
                        break;
                }
                return Optional.of(certAuthority);
            }catch (Exception e){
                log.error(e);
            }
        }
        return Optional.empty();
    }
}
