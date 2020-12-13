package com.winllc.pki.ra.service;

import com.winllc.acme.common.*;
import com.winllc.acme.common.ca.CertAuthority;
import com.winllc.acme.common.ca.ConnectionProperty;
import com.winllc.acme.common.domain.CertAuthorityConnectionInfo;
import com.winllc.acme.common.domain.CertAuthorityConnectionProperty;
import com.winllc.acme.common.ra.RACertificateIssueRequest;
import com.winllc.acme.common.ra.RACertificateRevokeRequest;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.beans.form.CertAuthorityConnectionInfoForm;
import com.winllc.pki.ra.beans.validator.ValidationResponse;
import com.winllc.pki.ra.ca.*;
import com.winllc.acme.common.contants.CertificateStatus;
import com.winllc.pki.ra.constants.AccountRestrictionType;
import com.winllc.pki.ra.domain.*;
import com.winllc.pki.ra.exception.InvalidFormException;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.*;
import com.winllc.pki.ra.service.transaction.CertIssuanceTransaction;
import com.winllc.pki.ra.service.transaction.CertRevocationTransaction;
import com.winllc.pki.ra.service.validators.CertAuthorityConnectionInfoValidator;
import io.github.classgraph.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.transaction.Transactional;
import javax.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ca")
@Transactional
public class CertAuthorityConnectionService extends AbstractService {

    private static final Logger log = LogManager.getLogger(CertAuthorityConnectionService.class);

    private final CertAuthorityConnectionInfoRepository repository;
    private final CertAuthorityConnectionPropertyRepository propertyRepository;
    private final AccountRepository accountRepository;
    private final LoadedCertAuthorityStore certAuthorityStore;
    private final CertAuthorityConnectionInfoValidator certAuthorityConnectionInfoValidator;
    @Autowired
    private AuthCredentialService authCredentialService;

    public CertAuthorityConnectionService(CertAuthorityConnectionInfoRepository repository,
                                          CertAuthorityConnectionPropertyRepository propertyRepository,
                                          AccountRepository accountRepository,
                                          ApplicationContext context, LoadedCertAuthorityStore certAuthorityStore,
                                          CertAuthorityConnectionInfoValidator certAuthorityConnectionInfoValidator) {
        super(context);
        this.repository = repository;
        this.propertyRepository = propertyRepository;
        this.accountRepository = accountRepository;
        this.certAuthorityStore = certAuthorityStore;
        this.certAuthorityConnectionInfoValidator = certAuthorityConnectionInfoValidator;
    }

    @InitBinder("certAuthorityConnectionInfoForm")
    public void initAppKeystoreEntryBinder(WebDataBinder binder) {
        binder.setValidator(certAuthorityConnectionInfoValidator);
    }

    @PostMapping("/api/info/create")
    @ResponseStatus(HttpStatus.CREATED)
    public Long createConnectionInfo(@Valid @RequestBody CertAuthorityConnectionInfoForm connectionInfo) {
        //todo allow required inputs on form before submitting here

        CertAuthorityConnectionInfo caConnection = new CertAuthorityConnectionInfo();
        caConnection.setName(connectionInfo.getName());
        caConnection.setCertAuthorityClassName(connectionInfo.getType());
        caConnection.setBaseUrl(connectionInfo.getBaseUrl());
        caConnection.setAuthKeyAlias(connectionInfo.getAuthKeyAlias());
        caConnection.setTrustChainBase64(connectionInfo.getTrustChainBase64());
        caConnection = repository.save(caConnection);

        certAuthorityStore.reload();

        Map<String, CertAuthorityConnectionProperty> propMap = connectionInfo.getProperties().stream()
                .collect(Collectors.toMap(p -> p.getName(), p -> p));

        //Create the required settings for the connection, will be filled in on edit screen
        Set<CertAuthorityConnectionProperty> props = new HashSet<>();
        for(ConnectionProperty connectionProperty : CertAuthority.getRequiredProperties()){
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

        return repository.findAll();
    }

    @GetMapping("/info/options")
    public Map<Long, String> optionsInfo(){
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

    private List<String> getCaOptions(){
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
            throws InvocationTargetException, IllegalAccessException,
            ClassNotFoundException, NoSuchMethodException {

        //todo clean this up
        Method m = Class.forName(connectionType).getMethod("getRequiredProperties");
        Object result = m.invoke(null);

        if(result instanceof List){
            log.info("Found a list");
            return (List<ConnectionProperty>) result;
        }

        return new ArrayList<>();

        /*
        Optional<CertAuthorityConnectionType> typeOptional = Stream.of(CertAuthorityConnectionType.values())
                .filter(v -> v.name().equalsIgnoreCase(connectionType))
                .findFirst();

        if(typeOptional.isPresent()){
            CertAuthorityConnectionType type = typeOptional.get();
            return type.getRequiredProperties();
        }else{
            throw new RAObjectNotFoundException(CertAuthorityConnectionType.class, connectionType);
        }

         */
    }


    @PostMapping("/issueCertificate")
    @ResponseStatus(HttpStatus.OK)
    public String issueCertificate(@Valid @RequestBody RACertificateIssueRequest raCertificateIssueRequest) throws Exception {

        if(raCertificateIssueRequest.getDnsNameList().size() == 0) throw new IllegalArgumentException("Must include at least one DNS name");

        CertIssuanceTransaction certIssuanceTransaction = new CertIssuanceTransaction(certAuthorityStore.getLoadedCertAuthority(
                raCertificateIssueRequest.getCertAuthorityName()), context);

        X509Certificate cert;

        Optional<Account> optionalAccount = authCredentialService.getAssociatedAccount(raCertificateIssueRequest.getAccountKid());
        if(optionalAccount.isPresent()) {
            Account account = optionalAccount.get();

            cert = certIssuanceTransaction.processIssueCertificate(raCertificateIssueRequest, account);
        }else{
            cert = certIssuanceTransaction.processIssueCertificate(raCertificateIssueRequest);
        }

        return CertUtil.formatCrtFileContents(cert);
    }

    @PostMapping("/revokeCertificate")
    @ResponseStatus(HttpStatus.OK)
    public void revokeCertificate(@Valid @RequestBody RACertificateRevokeRequest revokeRequest) throws Exception {
        CertAuthority certAuthority = certAuthorityStore.getLoadedCertAuthority(revokeRequest.getCertAuthorityName());
        if (certAuthority != null) {

            CertRevocationTransaction revocationTransaction = new CertRevocationTransaction(certAuthority, context);
            revocationTransaction.processRevokeCertificate(revokeRequest);

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
                CertificateStatus status = certAuthority.getCertificateStatus(serial);
                details.setCertificateBase64(CertUtil.formatCrtFileContents(cert));
                details.setStatus(status.name());
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

}
