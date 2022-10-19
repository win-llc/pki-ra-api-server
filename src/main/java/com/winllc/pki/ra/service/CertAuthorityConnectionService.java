package com.winllc.pki.ra.service;

import com.winllc.acme.common.*;
import com.winllc.acme.common.client.ca.LoadedCertAuthorityStore;
import com.winllc.acme.common.constants.DateTimeUtil;
import com.winllc.acme.common.domain.*;
import com.winllc.acme.common.ra.RACertificateIssueRequest;
import com.winllc.acme.common.ra.RACertificateRevokeRequest;
import com.winllc.acme.common.repository.CertAuthorityConnectionInfoRepository;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.beans.form.CertAuthorityConnectionInfoForm;
import com.winllc.pki.ra.beans.form.DomainForm;
import com.winllc.pki.ra.beans.info.CertAuthorityInfo;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.*;
import com.winllc.pki.ra.service.transaction.CertIssuanceTransaction;
import com.winllc.pki.ra.service.transaction.CertRevocationTransaction;
import com.winllc.pki.ra.service.validators.CertAuthorityConnectionInfoValidator;
import com.winllc.ra.integration.ca.*;
import io.github.classgraph.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import javax.validation.Valid;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ca")
@Transactional
public class CertAuthorityConnectionService extends
        DataPagedService<CertAuthorityConnectionInfo, CertAuthorityConnectionInfoForm,
                CertAuthorityConnectionInfoRepository> {

    private static final Logger log = LogManager.getLogger(CertAuthorityConnectionService.class);

    private final CertAuthorityConnectionInfoRepository repository;
    private final CertAuthorityConnectionPropertyRepository propertyRepository;
    private final LoadedCertAuthorityStore certAuthorityStore;
    private final CertAuthorityConnectionInfoValidator certAuthorityConnectionInfoValidator;
    private final AuthCredentialService authCredentialService;
    private final RevocationRequestService revocationRequestService;
    private final CertificateRequestRepository certificateRequestRepository;

    public CertAuthorityConnectionService(CertAuthorityConnectionInfoRepository repository,
                                          CertAuthorityConnectionPropertyRepository propertyRepository,
                                          ApplicationContext context, LoadedCertAuthorityStore certAuthorityStore,
                                          CertAuthorityConnectionInfoValidator certAuthorityConnectionInfoValidator,
                                          AuthCredentialService authCredentialService, RevocationRequestService revocationRequestService,
                                          CertificateRequestRepository certificateRequestRepository) {
        super(context, CertAuthorityConnectionInfo.class, repository);
        this.repository = repository;
        this.propertyRepository = propertyRepository;
        this.certAuthorityStore = certAuthorityStore;
        this.certAuthorityConnectionInfoValidator = certAuthorityConnectionInfoValidator;
        this.authCredentialService = authCredentialService;
        this.revocationRequestService = revocationRequestService;
        this.certificateRequestRepository = certificateRequestRepository;
    }

    @InitBinder("certAuthorityConnectionInfoForm")
    public void initAppKeystoreEntryBinder(WebDataBinder binder) {
        binder.setValidator(certAuthorityConnectionInfoValidator);
    }


    @GetMapping("/view/{name}")
    public CertAuthorityInfo getCertAuthorityInfo(@PathVariable String name) throws Exception {
        CertAuthority ca = certAuthorityStore.getLoadedCertAuthority(name);
        return buildInfo(ca);
    }

    @GetMapping("/all/info")
    public List<CertAuthorityInfo> getAllCertAuthorityInfo() {
        List<CertAuthority> allCertAuthorities = certAuthorityStore.getAllCertAuthorities();

        List<CertAuthorityInfo> infoList = new ArrayList<>();
        for (CertAuthority ca : allCertAuthorities) {
            try {
                infoList.add(buildInfo(ca));
            } catch (Exception e) {
                log.error("Could not build info", e);
            }
        }

        return infoList;
    }

    private CertAuthorityInfo buildInfo(CertAuthority ca) throws Exception {
        CertAuthorityInfo info = new CertAuthorityInfo();

        X509Certificate certificate = (X509Certificate) ca.getTrustChain()[0];

        info.setName(ca.getName());
        info.setTrustChain(ca.getConnectionInfo().getTrustChainBase64());
        info.setDn(certificate.getSubjectDN().getName());
        info.setValidFrom(DateTimeUtil.DATE_TIME_FORMATTER.format(certificate.getNotBefore().toInstant()));
        info.setValidTo(DateTimeUtil.DATE_TIME_FORMATTER.format(certificate.getNotAfter().toInstant()));

        return info;
    }

    @PostMapping("/api/info/create")
    @ResponseStatus(HttpStatus.CREATED)
    public Long createConnectionInfo(@Valid @RequestBody CertAuthorityConnectionInfoForm connectionInfo) throws Exception {
        //todo allow required inputs on form before submitting here

        CertAuthorityConnectionInfo caConnection = new CertAuthorityConnectionInfo();
        caConnection.setName(connectionInfo.getName());
        caConnection.setCertAuthorityClassName(connectionInfo.getType());
        caConnection.setBaseUrl(connectionInfo.getBaseUrl());
        caConnection.setAuthKeyAlias(connectionInfo.getAuthKeyAlias());
        caConnection.setTrustChainBase64(connectionInfo.getTrustChainBase64());
        caConnection = repository.save(caConnection);

        certAuthorityStore.loadCertAuthority(caConnection);

        Map<String, CertAuthorityConnectionProperty> propMap = connectionInfo.getProperties().stream()
                .collect(Collectors.toMap(p -> p.getName(), p -> p));

        //Create the required settings for the connection, will be filled in on edit screen
        List<ConnectionProperty> propertiesForClass = getPropertiesForClass(Class.forName(connectionInfo.getType()));
        Set<CertAuthorityConnectionProperty> props = new HashSet<>();
        for (ConnectionProperty connectionProperty : propertiesForClass) {
            CertAuthorityConnectionProperty prop = new CertAuthorityConnectionProperty();
            prop.setName(connectionProperty.getName());

            if (propMap.containsKey(connectionProperty.getName())) {
                prop.setValue(propMap.get(connectionProperty.getName()).getValue());
            } else {
                prop.setValue("");
            }

            prop.setCertAuthorityConnectionInfo(caConnection);
            prop = propertyRepository.save(prop);
            props.add(prop);
        }

        caConnection.setProperties(props);
        caConnection = repository.save(caConnection);
        certAuthorityStore.loadCertAuthority(caConnection);

        return caConnection.getId();
    }

    @Transactional
    @PostMapping("/api/info/update")
    @ResponseStatus(HttpStatus.OK)
    public CertAuthorityConnectionInfoForm updateConnectionInfo(@Valid @RequestBody CertAuthorityConnectionInfoForm form)
            throws RAException {

        Optional<CertAuthorityConnectionInfo> optionalInfo = repository.findById(form.getId());
        if (optionalInfo.isPresent()) {
            final CertAuthorityConnectionInfo info = optionalInfo.get();

            Set<CertAuthorityConnectionProperty> props = new HashSet<>();
            if (!CollectionUtils.isEmpty(form.getProperties())) {
                for (CertAuthorityConnectionProperty prop : form.getProperties()) {
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

            certAuthorityStore.loadCertAuthority(info2);

            return buildForm(info2);
        } else {
            throw new RAObjectNotFoundException(CertAuthorityConnectionInfo.class, form.getId());
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

    private CertAuthorityConnectionInfoForm buildForm(CertAuthorityConnectionInfo info) {
        Hibernate.initialize(info.getProperties());
        CertAuthority ca = certAuthorityStore.getLoadedCertAuthority(info.getName());
        return new CertAuthorityConnectionInfoForm(info, ca);
    }

    @GetMapping("/api/info/all")
    @ResponseStatus(HttpStatus.OK)
    public List<CertAuthorityConnectionInfo> getAllConnectionInfo() {

        return repository.findAll();
    }

    @GetMapping("/info/options")
    public Map<Long, String> optionsInfo() {
        return repository.findAll().stream()
                .collect(Collectors.toMap(d -> d.getId(), d -> d.getName()));
    }

    @DeleteMapping("/api/info/delete/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteInfo(@PathVariable Long id) {
        repository.deleteById(id);
    }

    @GetMapping("/api/info/getTypes")
    @ResponseStatus(HttpStatus.OK)
    public List<String> getTypes() {

        return getCaOptions();
    }

    private List<String> getCaOptions() {
        //todo externalize
        String pkg = "com.winllc.pki.plugins";
        try (ScanResult scanResult = new ClassGraph().enableAllInfo().acceptPackages(pkg)
                .scan()) {
            ClassInfoList widgetClasses = scanResult
                    .getClassesImplementing(CertAuthority.class.getCanonicalName());

            List<ClassInfo> classInfoList = widgetClasses.stream()
                    .filter(ci -> !ci.isAbstract())
                    .collect(Collectors.toList());
            List<String> caClassNames = classInfoList.stream()
                    .map(ci -> ci.getName())
                    .collect(Collectors.toList());
            // ...
            return caClassNames;
        }
    }

    @GetMapping("/api/info/getRequiredPropertiesForType/{connectionType}")
    public List<ConnectionProperty> getRequiredPropertiesForType(@PathVariable String connectionType)
            throws Exception {

        return getPropertiesForClass(Class.forName(connectionType));
    }

    private List<ConnectionProperty> getPropertiesForClass(Class caClass) throws Exception {
        Method m = caClass.getMethod("getRequiredProperties");
        Object result = m.invoke(null);

        if (result instanceof List) {
            log.info("Found a list");
            return (List<ConnectionProperty>) result;
        }

        return new ArrayList<>();
    }


    @PostMapping("/issueCertificate")
    @ResponseStatus(HttpStatus.OK)
    public String issueCertificate(@Valid @RequestBody RACertificateIssueRequest raCertificateIssueRequest) throws Exception {

        if (raCertificateIssueRequest.getDnsNameList().size() == 0)
            throw new IllegalArgumentException("Must include at least one DNS name");

        CertAuthority certAuthority = certAuthorityStore.getLoadedCertAuthority(raCertificateIssueRequest.getCertAuthorityName());

        if (certAuthority == null) {
            throw new RAObjectNotFoundException(CertAuthority.class, raCertificateIssueRequest.getCertAuthorityName());
        }

        CertIssuanceTransaction certIssuanceTransaction = new CertIssuanceTransaction(certAuthority, context);

        X509Certificate cert;

        Optional<Account> optionalAccount = authCredentialService.getAssociatedAccount(raCertificateIssueRequest.getAccountKid());
        if (optionalAccount.isPresent()) {
            Account account = optionalAccount.get();

            cert = certIssuanceTransaction.processIssueCertificate(raCertificateIssueRequest, account);
        } else {
            cert = certIssuanceTransaction.processIssueCertificate(raCertificateIssueRequest);
        }

        return CertUtil.formatCrtFileContents(cert);
    }

    @PostMapping("/revokeCertificate")
    @ResponseStatus(HttpStatus.OK)
    public void revokeCertificate(@Valid @RequestBody RACertificateRevokeRequest revokeRequest) throws Exception {
        CertAuthority certAuthority = certAuthorityStore.getLoadedCertAuthority(revokeRequest.getCertAuthorityName());
        if (certAuthority != null) {

            String serial = revokeRequest.getSerial();
            if (StringUtils.isEmpty(serial)) {
                serial = getSerialFromRequest(revokeRequest);
            }

            RevocationRequest request = new RevocationRequest("acme-server");
            request.setSerial(serial);
            request.setIssuerDn(certAuthority.getIssuerName().toString());
            request.setReason(revokeRequest.getReason());
            request.setStatus("new");
            request = revocationRequestService.save(request);

            CertRevocationTransaction revocationTransaction = new CertRevocationTransaction(certAuthority, context);
            revocationTransaction.processRevokeCertificate(request);

        } else {
            throw new RAObjectNotFoundException(CertAuthority.class, revokeRequest.getCertAuthorityName());
        }
    }

    @GetMapping("/validationRules/{connectionName}")
    @ResponseStatus(HttpStatus.OK)
    public CertIssuanceValidationResponse getValidationRules(@PathVariable String connectionName) throws RAObjectNotFoundException {
        CertAuthority certAuthority = certAuthorityStore.getLoadedCertAuthority(connectionName);
        if (certAuthority != null) {
            CertIssuanceValidationResponse response = new CertIssuanceValidationResponse();
            //todo get global validation rules from connection info

            CertAuthorityConnectionInfoInterface info = certAuthority.getConnectionInfo();

            //response.getCertIssuanceValidationRules().add();

            return response;
        } else {
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
                CertificateStatus status = certAuthority.getCertificateStatus(serial);
                details.setCertificateBase64(CertUtil.formatCrtFileContents(cert));
                details.setStatus(status.name());
                details.setSerial(serial);

                return details;
            } else {
                throw new RAException("Could not find certificate with serial: " + serial);
            }
        } else {
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

    public Optional<CertAuthority> getCertAuthorityByIssuerDn(String issuerDn) throws InvalidNameException {
        LdapName issuerName = new LdapName(issuerDn);

        return certAuthorityStore.getAllCertAuthorities()
                .stream()
                .filter(ca -> {
                    try {
                        return ca.getIssuerName().equals(issuerName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return false;
                }).findFirst();
    }

    private String getSerialFromRequest(RACertificateRevokeRequest revokeRequest) throws RAException, CertificateException, IOException {
        Optional<CertificateRequest> optionalCertificateRequest = certificateRequestRepository.findById(revokeRequest.getRequestId());
        if (optionalCertificateRequest.isPresent()) {
            CertificateRequest certificateRequest = optionalCertificateRequest.get();
            if (StringUtils.isNotBlank(certificateRequest.getIssuedCertificate())) {
                X509Certificate x509Certificate = CertUtil.base64ToCert(certificateRequest.getIssuedCertificate());
                return x509Certificate.getSerialNumber().toString();
            } else {
                throw new RAException("No certificate in request, most likely not issued yet");
            }
        } else {
            throw new RAObjectNotFoundException(CertificateRequest.class, revokeRequest.getRequestId());
        }
    }

    @Override
    protected CertAuthorityConnectionInfoForm entityToForm(CertAuthorityConnectionInfo entity) {
        CertAuthority loadedCertAuthority = certAuthorityStore.getLoadedCertAuthority(entity.getName());
        return new CertAuthorityConnectionInfoForm(entity, loadedCertAuthority);
    }

    @Override
    protected CertAuthorityConnectionInfo formToEntity(CertAuthorityConnectionInfoForm connectionInfo,
                                                       Authentication authentication)
            throws Exception {
        CertAuthorityConnectionInfo caConnection = new CertAuthorityConnectionInfo();
        caConnection.setName(connectionInfo.getName());
        caConnection.setCertAuthorityClassName(connectionInfo.getType());
        caConnection.setBaseUrl(connectionInfo.getBaseUrl());
        caConnection.setAuthKeyAlias(connectionInfo.getAuthKeyAlias());
        caConnection.setTrustChainBase64(connectionInfo.getTrustChainBase64());
        caConnection = repository.save(caConnection);

        certAuthorityStore.loadCertAuthority(caConnection);

        Map<String, CertAuthorityConnectionProperty> propMap = connectionInfo.getProperties().stream()
                .collect(Collectors.toMap(p -> p.getName(), p -> p));

        //Create the required settings for the connection, will be filled in on edit screen
        List<ConnectionProperty> propertiesForClass = getPropertiesForClass(Class.forName(connectionInfo.getType()));
        Set<CertAuthorityConnectionProperty> props = new HashSet<>();
        for (ConnectionProperty connectionProperty : propertiesForClass) {
            CertAuthorityConnectionProperty prop = new CertAuthorityConnectionProperty();
            prop.setName(connectionProperty.getName());

            if (propMap.containsKey(connectionProperty.getName())) {
                prop.setValue(propMap.get(connectionProperty.getName()).getValue());
            } else {
                prop.setValue("");
            }

            prop.setCertAuthorityConnectionInfo(caConnection);
            prop = propertyRepository.save(prop);
            props.add(prop);
        }

        caConnection.setProperties(props);
        caConnection = repository.save(caConnection);
        certAuthorityStore.loadCertAuthority(caConnection);
        return caConnection;
    }

    @Override
    protected CertAuthorityConnectionInfo combine(CertAuthorityConnectionInfo original,
                                                  CertAuthorityConnectionInfo updated, Authentication authentication) {

        Set<CertAuthorityConnectionProperty> props = new HashSet<>();
        if (!CollectionUtils.isEmpty(updated.getProperties())) {
            for (CertAuthorityConnectionProperty prop : updated.getProperties()) {
                prop.setCertAuthorityConnectionInfo(original);
                prop = propertyRepository.save(prop);
                props.add(prop);
            }
        }
        original.setProperties(props);
        original.setBaseUrl(updated.getBaseUrl());
        original.setAuthKeyAlias(updated.getAuthKeyAlias());
        original.setTrustChainBase64(updated.getTrustChainBase64());

        original = repository.save(original);

        certAuthorityStore.loadCertAuthority(original);

        return original;
    }

    @Override
    public List<Predicate> buildFilter(Map<String, String> allRequestParams, Root<CertAuthorityConnectionInfo> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return null;
    }
}
