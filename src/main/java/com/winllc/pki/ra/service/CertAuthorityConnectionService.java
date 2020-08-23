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
import com.winllc.pki.ra.service.external.EntityDirectoryService;
import com.winllc.pki.ra.service.transaction.CertIssuanceTransaction;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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

import static com.winllc.pki.ra.constants.ServerSettingRequired.ENTITY_DIRECTORY_LDAP_SERVERBASEDN;

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
    private CertAuthorityConnectionInfoValidator validator;
    @Autowired
    private AuditRecordRepository auditRecordRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CertificateRequestRepository certificateRequestRepository;
    @Autowired
    private ApplicationContext context;
    @Autowired
    private LoadedCertAuthorityStore certAuthorityStore;

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

            certAuthorityStore.reload();

            CertAuthority ca = certAuthorityStore.getLoadedCertAuthority(caConnection.getName());

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
            certAuthorityStore.reload();

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
        CertAuthority ca = certAuthorityStore.getLoadedCertAuthority(info.getName());
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

            CertIssuanceTransaction certIssuanceTransaction = new CertIssuanceTransaction(certAuthorityStore.getLoadedCertAuthority(
                    raCertificateIssueRequest.getCertAuthorityName()), context);

            X509Certificate cert = certIssuanceTransaction.processIssueCertificate(raCertificateIssueRequest, account);
            String pemCert = CertUtil.formatCrtFileContents(cert);

            return pemCert;
        }else{
            throw new RAObjectNotFoundException(Account.class, raCertificateIssueRequest.getAccountKid());
        }
    }



    @PostMapping("/revokeCertificate")
    @ResponseStatus(HttpStatus.OK)
    public void revokeCertificate(@Valid @RequestBody RACertificateRevokeRequest revokeRequest) throws Exception {
        CertAuthority certAuthority = certAuthorityStore.getLoadedCertAuthority(revokeRequest.getCertAuthorityName());
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
        CertAuthority certAuthority = certAuthorityStore.getLoadedCertAuthority(connectionName);
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

        CertAuthority certAuthority = certAuthorityStore.getLoadedCertAuthority(connectionName);
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
        CertAuthority certAuthority = certAuthorityStore.getLoadedCertAuthority(connectionName);

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
        CertAuthority certAuthority = certAuthorityStore.getLoadedCertAuthority(connectionName);

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
        CertAuthority ca = certAuthorityStore.getLoadedCertAuthority(name);

        if (ca != null) {
            return Optional.of(ca);
        } else {
            return Optional.empty();
        }
    }

}
