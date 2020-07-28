package com.winllc.pki.ra.service;

import com.winllc.acme.common.*;
import com.winllc.acme.common.ra.RACertificateIssueRequest;
import com.winllc.acme.common.ra.RACertificateRevokeRequest;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.beans.form.CertAuthorityConnectionInfoForm;
import com.winllc.pki.ra.beans.form.ServerEntryForm;
import com.winllc.pki.ra.beans.validator.CertAuthorityConnectionInfoValidator;
import com.winllc.pki.ra.beans.validator.ValidationResponse;
import com.winllc.pki.ra.ca.*;
import com.winllc.pki.ra.constants.AuditRecordType;
import com.winllc.pki.ra.domain.*;
import com.winllc.pki.ra.exception.InvalidFormException;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.keystore.ApplicationKeystore;
import com.winllc.pki.ra.repository.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.transaction.Transactional;
import javax.validation.Valid;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
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
    @Autowired
    private ServerEntryRepository serverEntryRepository;
    @Autowired
    private ServerEntryService serverEntryService;

    private static final Map<String, CertAuthority> loadedCertAuthorities = new ConcurrentHashMap<>();

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

            loadedCertAuthorities.put(ca.getName(), ca);
        }
    }

    public CertAuthority getLoadedCertAuthority(String name){
        return loadedCertAuthorities.get(name);
    }

    public void addLoadedCertAuthority(CertAuthority ca){
        loadedCertAuthorities.put(ca.getName(), ca);
    }

    //todo CertAuthorityConnectionTemplate?


    @PostMapping("/api/info/create")
    @ResponseStatus(HttpStatus.CREATED)
    public Long createConnectionInfo(@Valid @RequestBody CertAuthorityConnectionInfoForm connectionInfo) throws InvalidFormException {
        //todo allow required inputs on form before submitting here
        ValidationResponse validationResponse = validator.validate(connectionInfo, false);

        if (validationResponse.isValid()) {
            CertAuthorityConnectionInfo caConnection = new CertAuthorityConnectionInfo();
            caConnection.setName(connectionInfo.getName());
            caConnection.setType(CertAuthorityConnectionType.valueOf(connectionInfo.getType()));
            caConnection.setBaseUrl(connectionInfo.getBaseUrl());
            caConnection.setAuthKeyAlias(connectionInfo.getAuthKeyAlias());
            caConnection.setTrustChainBase64(connectionInfo.getTrustChainBase64());
            caConnection = repository.save(caConnection);

            loadCertAuthority(caConnection.getName());

            CertAuthority ca = getLoadedCertAuthority(caConnection.getName());

            Map<String, CertAuthorityConnectionProperty> propMap = connectionInfo.getProperties().stream()
                    .collect(Collectors.toMap(p -> p.getName(), p -> p));

            //Create the required settings for the connection, will be filled in on edit screen
            Set<CertAuthorityConnectionProperty> props = new HashSet<>();
            for(ConnectionProperty connectionProperty : ca.getType().getRequiredProperties()){
                CertAuthorityConnectionProperty prop = new CertAuthorityConnectionProperty();
                prop.setName(connectionProperty.getName());

                if(propMap.containsKey(connectionProperty.getName())){
                    prop.setValue(propMap.get(connectionProperty.getName()).getValue());
                }else {
                    prop.setValue("");
                }

                prop.setCertAuthorityConnectionInfo(caConnection);
                prop = propertyRepository.save(prop);
                props.add(prop);
            }

            caConnection.setProperties(props);
            caConnection = repository.save(caConnection);

            //reload cert authority
            loadCertAuthority(caConnection.getName());

            return caConnection.getId();
        } else {
            throw new InvalidFormException(connectionInfo);
        }
    }

    @Transactional
    @PostMapping("/api/info/update")
    @ResponseStatus(HttpStatus.OK)
    public CertAuthorityConnectionInfoForm updateConnectionInfo(@Valid @RequestBody CertAuthorityConnectionInfoForm form)
            throws RAException {
        //todo validate

        ValidationResponse validationResponse = validator.validate(form, true);

        if(validationResponse.isValid()) {

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
                info.setBaseUrl(form.getBaseUrl());
                info.setAuthKeyAlias(form.getAuthKeyAlias());
                info.setTrustChainBase64(form.getTrustChainBase64());

                CertAuthorityConnectionInfo info2 = repository.save(info);
                return buildForm(info2);
            }else{
                throw new RAObjectNotFoundException(CertAuthorityConnectionInfo.class, form.getId());
            }
        }else{
            throw new InvalidFormException(form);
        }
    }

    @GetMapping("/api/info/byName/{name}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public CertAuthorityConnectionInfoForm getConnectionInfoByName(@PathVariable String name) throws RAObjectNotFoundException {
        Optional<CertAuthorityConnectionInfo> infoOptional = repository.findByName(name);

        if (infoOptional.isPresent()) {
            return buildForm(infoOptional.get());
        } else {
            throw new RAObjectNotFoundException(CertAuthorityConnectionInfo.class, name);
        }
    }

    @GetMapping("/api/info/byId/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public CertAuthorityConnectionInfoForm getConnectionInfoById(@PathVariable Long id) throws RAObjectNotFoundException {
        Optional<CertAuthorityConnectionInfo> infoOptional = repository.findById(id);

        if (infoOptional.isPresent()) {
            return buildForm(infoOptional.get());
        } else {
            throw new RAObjectNotFoundException(CertAuthorityConnectionInfo.class, id);
        }
    }

    private CertAuthorityConnectionInfoForm buildForm(CertAuthorityConnectionInfo info){
        Hibernate.initialize(info.getProperties());
        CertAuthority ca = getLoadedCertAuthority(info.getName());
        return new CertAuthorityConnectionInfoForm(info, ca);
    }

    @GetMapping("/api/info/all")
    @ResponseStatus(HttpStatus.OK)
    public List<CertAuthorityConnectionInfo> getAllConnectionInfo() {
        List<CertAuthorityConnectionInfo> list = repository.findAll();

        return list;
    }

    @DeleteMapping("/api/info/delete/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteInfo(@PathVariable Long id) {
        repository.deleteById(id);
    }

    @GetMapping("/api/info/getTypes")
    @ResponseStatus(HttpStatus.OK)
    public List<String> getTypes() {
        List<String> certAuthorityTypes = Stream.of(CertAuthorityConnectionType.values())
                .map(v -> v.name())
                .collect(Collectors.toList());

        return certAuthorityTypes;
    }

    @GetMapping("/api/info/getRequiredPropertiesForType/{connectionType}")
    public List<ConnectionProperty> getRequiredPropertiesForType(@PathVariable String connectionType) throws RAObjectNotFoundException {
        Optional<CertAuthorityConnectionType> typeOptional = Stream.of(CertAuthorityConnectionType.values())
                .filter(v -> v.name().equalsIgnoreCase(connectionType))
                .findFirst();

        if(typeOptional.isPresent()){
            CertAuthorityConnectionType type = typeOptional.get();
            return type.getRequiredProperties();
        }else{
            throw new RAObjectNotFoundException(CertAuthorityConnectionType.class, connectionType);
        }
    }

    @PostMapping("/issueCertificate")
    @ResponseStatus(HttpStatus.OK)
    public String issueCertificate(@Valid @RequestBody RACertificateIssueRequest raCertificateIssueRequest) throws Exception {

        if(raCertificateIssueRequest.getDnsNameList().size() == 0) throw new IllegalArgumentException("Must include at least one DNS name");

        //todo account shouldn't be required of EAB not required
        Optional<Account> optionalAccount = accountRepository.findByKeyIdentifierEquals(raCertificateIssueRequest.getAccountKid());
        if(optionalAccount.isPresent()) {
            Account account = optionalAccount.get();
            X509Certificate cert = processIssueCertificate(raCertificateIssueRequest, account);
            String pemCert = CertUtil.formatCrtFileContents(cert);

            return pemCert;
        }else{
            throw new RAObjectNotFoundException(Account.class, raCertificateIssueRequest.getAccountKid());
        }
    }

    public X509Certificate processIssueCertificate(RACertificateIssueRequest certificateRequest, Account account) throws Exception {
        //todo validation checks, probably before this, notify POCs on failure
        CertAuthority certAuthority = getLoadedCertAuthority(certificateRequest.getCertAuthorityName());

        SubjectAltNames subjectAltNames = new SubjectAltNames();
        for (String dnsName : certificateRequest.getDnsNameList()) {
            subjectAltNames.addValue(SubjectAltNames.SubjAltNameType.DNS, dnsName);
        }

        String buildDn = buildDn(certificateRequest, account);

        X509Certificate cert = certAuthority.issueCertificate(certificateRequest.getCsr(), buildDn, subjectAltNames);
        if (cert != null) {
            processIssuedCertificate(cert, certificateRequest, account);
            return cert;
        } else {
            throw new RAException("Could not issue certificate");
        }
    }

    private String buildDn(RACertificateIssueRequest certificateRequest, Account account){
        String dn = "CN=";

        if(StringUtils.isNotBlank(certificateRequest.getSubjectNameRequest())){
            dn += certificateRequest.getSubjectNameRequest();
        }else{
            //select first fqdn
            dn += certificateRequest.getDnsNameList().get(0);
        }

        if(StringUtils.isNotBlank(account.getEntityBaseDn())) {
            dn += "," + account.getEntityBaseDn();
        }
        return dn;
    }

    public void processIssuedCertificate(X509Certificate certificate, RACertificateIssueRequest raCertificateIssueRequest, Account account)
            throws CertificateEncodingException, RAObjectNotFoundException {

        ServerEntry serverEntry;
        Optional<ServerEntry> optionalServerEntry =
                serverEntryRepository.findDistinctByDistinguishedNameIgnoreCaseAndAccount(certificate.getSubjectDN().getName(), account);

        if(optionalServerEntry.isPresent()){
            serverEntry = optionalServerEntry.get();
        }else{
            String fqdn = raCertificateIssueRequest.getDnsNameList().get(0);

            ServerEntryForm form = new ServerEntryForm();
            form.setAccountId(account.getId());
            form.setFqdn(fqdn);
            form.setAlternateDnsValues(raCertificateIssueRequest.getDnsNameList());
            Long serverEntryId = serverEntryService.createServerEntry(form);
            serverEntry = serverEntryRepository.findById(serverEntryId).get();
        }

        //add as certificate request
        CertificateRequest certificateRequest;
        if(raCertificateIssueRequest.getExistingCertificateRequestId() == null) {
            certificateRequest = new CertificateRequest();
            certificateRequest.setAccount(account);
            certificateRequest.setCsr(raCertificateIssueRequest.getCsr());
            certificateRequest = certificateRequestRepository.save(certificateRequest);
        }else{
            Optional<CertificateRequest> optionalRequest
                    = certificateRequestRepository.findById(raCertificateIssueRequest.getExistingCertificateRequestId());
            if(optionalRequest.isPresent()){
                certificateRequest = optionalRequest.get();
            }else{
                log.error("Expected an existing certificate request, but found none");
                throw new RAObjectNotFoundException(CertificateRequest.class, raCertificateIssueRequest.getExistingCertificateRequestId());
            }
        }

        certificateRequest.setCertAuthorityName(raCertificateIssueRequest.getCertAuthorityName());
        certificateRequest.setStatus("issued");
        certificateRequest.setSubmittedOn(Timestamp.valueOf(LocalDateTime.now()));
        certificateRequest.addIssuedCertificate(certificate);
        certificateRequest.setServerEntry(serverEntry);
        certificateRequest = certificateRequestRepository.save(certificateRequest);

        //todo save public key info to certificate request for easy lookup

        serverEntry.setDistinguishedName(certificate.getSubjectDN().getName());
        serverEntry.getCertificateRequests().add(certificateRequest);
        serverEntry = serverEntryRepository.save(serverEntry);

        //todo consolidate this somewhere else
        AuditRecord record = AuditRecord.buildNew(AuditRecordType.CERTIFICATE_ISSUED);
        record.setAccountKid(raCertificateIssueRequest.getAccountKid());
        record.setSource(raCertificateIssueRequest.getSource());
        record.setObjectClass(serverEntry.getClass().getCanonicalName());
        record.setObjectUuid(serverEntry.getUuid().toString());
        auditRecordRepository.save(record);
    }

    @PostMapping("/revokeCertificate")
    @ResponseStatus(HttpStatus.OK)
    public void revokeCertificate(@Valid @RequestBody RACertificateRevokeRequest revokeRequest) throws Exception {
        CertAuthority certAuthority = getLoadedCertAuthority(revokeRequest.getCertAuthorityName());
        if (certAuthority != null) {
            String serial = revokeRequest.getSerial();
            //Get serial from certificate request
            if(StringUtils.isBlank(serial)){
                serial = getSerialFromRequest(revokeRequest);
            }

            if(StringUtils.isBlank(serial)) throw new Exception("Request ID and Serial can't both be null");

            boolean revoked = certAuthority.revokeCertificate(serial, revokeRequest.getReason());
            if (revoked) {
                //todo consolidate this somewhere else
                AuditRecord record = AuditRecord.buildNew(AuditRecordType.CERTIFICATE_REVOKED);
                //todo get account KID and add to record
                auditRecordRepository.save(record);
            } else {
                throw new RAException("Could not revoke cert on Cert Authority");
            }
        } else {
            throw new RAObjectNotFoundException(CertAuthority.class, revokeRequest.getCertAuthorityName());
        }
    }

    private String getSerialFromRequest(RACertificateRevokeRequest revokeRequest) throws RAException, CertificateException, IOException {
        Optional<CertificateRequest> optionalCertificateRequest = certificateRequestRepository.findById(revokeRequest.getRequestId());
        if(optionalCertificateRequest.isPresent()){
            CertificateRequest certificateRequest = optionalCertificateRequest.get();
            if(StringUtils.isNotBlank(certificateRequest.getIssuedCertificate())){
                X509Certificate x509Certificate = CertUtil.base64ToCert(certificateRequest.getIssuedCertificate());
                return x509Certificate.getSerialNumber().toString();
            }else{
                throw new RAException("No certificate in request, most likely not issued yet");
            }
        }else{
            throw new RAObjectNotFoundException(CertificateRequest.class, revokeRequest.getRequestId());
        }
    }

    @GetMapping("/validationRules/{connectionName}")
    @ResponseStatus(HttpStatus.OK)
    public CertIssuanceValidationResponse getValidationRules(@PathVariable String connectionName) throws RAObjectNotFoundException {
        CertAuthority certAuthority = getLoadedCertAuthority(connectionName);
        if (certAuthority != null) {
            CertIssuanceValidationResponse response = new CertIssuanceValidationResponse();
            //todo get global validation rules from connection info

            CertAuthorityConnectionInfo info = certAuthority.getConnectionInfo();

            //response.getCertIssuanceValidationRules().add();

            return response;
        }else{
            throw new RAObjectNotFoundException(CertAuthority.class, connectionName);
        }
    }

    @GetMapping("/certDetails/{connectionName}")
    @ResponseStatus(HttpStatus.OK)
    public CertificateDetails getCertificateStatus(@PathVariable String connectionName, @RequestParam String serial) throws Exception {

        CertAuthority certAuthority = getLoadedCertAuthority(connectionName);
        if (certAuthority != null) {
            CertificateDetails details = new CertificateDetails();
            X509Certificate cert = certAuthority.getCertificateBySerial(serial);
            if (cert != null) {
                String status = certAuthority.getCertificateStatus(serial);
                details.setCertificateBase64(CertUtil.formatCrtFileContents(cert));
                details.setStatus(status);
                details.setSerial(serial);

                return details;
            }else{
                throw new RAException("Could not find certificate with serial: "+serial);
            }
        }else{
            throw new RAObjectNotFoundException(CertAuthority.class, connectionName);
        }
    }

    @GetMapping("/trustChain/{connectionName}")
    @ResponseStatus(HttpStatus.OK)
    public String getTrustChain(@PathVariable String connectionName) throws Exception {
        CertAuthority certAuthority = getLoadedCertAuthority(connectionName);

        Certificate[] trustChain = certAuthority.getTrustChain();

        StringBuilder stringBuilder = new StringBuilder();
        for (Certificate cert : trustChain) {
            try {
                stringBuilder.append(CertUtil.formatCrtFileContents(cert)).append("\n");
            } catch (CertificateEncodingException e) {
                log.error("Could not build cert", e);
            }
        }
        return stringBuilder.toString();
    }


    @PostMapping("/search/{connectionName}")
    @ResponseStatus(HttpStatus.OK)
    public List<CertificateDetails> search(@PathVariable String connectionName, @RequestBody CertSearchParam certSearchParam) {
        CertAuthority certAuthority = getLoadedCertAuthority(connectionName);

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

        return certAuthority.search(certSearchParam);
    }

    public Optional<CertAuthority> getCertAuthorityByName(String name) {
        CertAuthority ca = getLoadedCertAuthority(name);

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
                        certAuthority = new InternalCertAuthority(info, entityManagerFactory);
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
