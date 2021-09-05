package com.winllc.pki.ra.service;

import com.winllc.acme.common.SubjectAltNames;
import com.winllc.acme.common.ca.CertAuthority;
import com.winllc.acme.common.ca.LoadedCertAuthorityStore;
import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.ra.RACertificateIssueRequest;
import com.winllc.pki.ra.beans.form.CertificateRequestDecisionForm;
import com.winllc.pki.ra.beans.form.CertificateRequestForm;
import com.winllc.pki.ra.beans.info.CertificateRequestInfo;
import com.winllc.pki.ra.beans.info.DomainInfo;
import com.winllc.pki.ra.beans.validator.CertRequestFormValidator;
import com.winllc.pki.ra.beans.validator.ValidationResponse;
import com.winllc.acme.common.domain.*;
import com.winllc.pki.ra.exception.InvalidFormException;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.*;
import com.winllc.pki.ra.service.transaction.CertIssuanceTransaction;
import com.winllc.pki.ra.service.validators.CertificateRequestDecisionValidator;
import com.winllc.pki.ra.service.validators.CertificateRequestValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.naming.InvalidNameException;
import javax.transaction.Transactional;
import javax.validation.Valid;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/certificateRequest")
public class CertificateRequestService extends AbstractService {

    private static final Logger log = LogManager.getLogger(CertificateRequestService.class);

    private final CertificateRequestRepository requestRepository;
    private final AccountRepository accountRepository;
    private final LoadedCertAuthorityStore certAuthorityStore;
    private final CertRequestFormValidator formValidator;
    private final CertificateRequestValidator certificateRequestValidator;
    private final CertificateRequestDecisionValidator certificateRequestDecisionValidator;
    private final ServerEntryRepository serverEntryRepository;
    private final DomainService domainService;

    public CertificateRequestService(CertificateRequestRepository requestRepository,
                                     AccountRepository accountRepository, LoadedCertAuthorityStore certAuthorityStore,
                                     CertRequestFormValidator formValidator, ApplicationContext context,
                                     CertificateRequestValidator certificateRequestValidator,
                                     CertificateRequestDecisionValidator certificateRequestDecisionValidator,
                                     ServerEntryRepository serverEntryRepository, DomainService domainService) {
        super(context);
        this.requestRepository = requestRepository;
        this.accountRepository = accountRepository;
        this.certAuthorityStore = certAuthorityStore;
        this.formValidator = formValidator;
        this.certificateRequestValidator = certificateRequestValidator;
        this.certificateRequestDecisionValidator = certificateRequestDecisionValidator;
        this.serverEntryRepository = serverEntryRepository;
        this.domainService = domainService;
    }

    @InitBinder("certificateRequestForm")
    public void initBinder(WebDataBinder binder) {
        binder.setValidator(certificateRequestValidator);
    }

    @InitBinder("certificateRequestDecisionForm")
    public void initDecisionBinder(WebDataBinder binder) {
        binder.setValidator(certificateRequestDecisionValidator);
    }

    public Optional<CertificateRequest> findCertificateRequestWithCertificate(X509Certificate certificate){

        try {
            Optional<CertAuthority> optionalCertAuthority
                    = certAuthorityStore.getLoadedCertAuthorityByIssuerDN(certificate.getIssuerDN());

            if(optionalCertAuthority.isPresent()){
                CertAuthority certAuthority = optionalCertAuthority.get();
                String foundCa = certAuthority.getName();
                return requestRepository
                        .findDistinctByIssuedCertificateSerialAndCertAuthorityName(
                                certificate.getSerialNumber().toString(), foundCa);
            }
        } catch (InvalidNameException e) {
            log.error("Could not find CA", e);
        }

        return Optional.empty();
    }

    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public List<CertificateRequest> getAll() {
        return requestRepository.findAll();
    }

    @GetMapping("/allWithStatus/{status}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<CertificateRequest> getAllWithStatus(@PathVariable String status) {
        List<CertificateRequest> requests = requestRepository.findAllByStatusEquals(status);

        requests.forEach(r -> Hibernate.initialize(r.getRequestedDnsNames()));

        return requests;
    }

    @GetMapping("/allWithStatus/{status}/count")
    @ResponseStatus(HttpStatus.OK)
    public Integer findByStatusCount(@PathVariable String status){
        return requestRepository.countAllByStatusEquals(status);
    }


    @GetMapping("/byId/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CertificateRequestInfo byId(@PathVariable Long id) throws RAObjectNotFoundException {
        Optional<CertificateRequest> byId = requestRepository.findById(id);

        if (byId.isPresent()) {
            return new CertificateRequestInfo(byId.get());
        } else {
            throw new RAObjectNotFoundException(CertificateRequest.class, id);
        }
    }

    @GetMapping("/byIdFull/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public CertificateRequest byIdFull(@PathVariable Long id) throws RAObjectNotFoundException {
        Optional<CertificateRequest> byId = requestRepository.findById(id);

        if (byId.isPresent()) {
            CertificateRequest certificateRequest = byId.get();
            Hibernate.initialize(certificateRequest.getRequestedDnsNames());
            return certificateRequest;
        } else {
            throw new RAObjectNotFoundException(CertificateRequest.class, id);
        }
    }

    @Transactional
    @PostMapping("/submit")
    @ResponseStatus(HttpStatus.CREATED)
    public Long submitRequest(@Valid @RequestBody CertificateRequestForm form, @AuthenticationPrincipal UserDetails raUser)
            throws Exception {
        ValidationResponse validationResponse = formValidator.validate(form, false);
        if (validationResponse.isValid()) {

            String fqdn = buildFqdn(form);

            CertificateRequest certificateRequest = CertificateRequest.build();
            certificateRequest.setCsr(form.getCsr());
            certificateRequest.setCertAuthorityName(form.getCertAuthorityName());
            certificateRequest.setPrimaryDnsName(fqdn);
            certificateRequest.setRequestedDnsNames(form.getRequestedDnsNames().stream()
                    .map(d -> d.getValue())
                    .collect(Collectors.joining(",")));

            Optional<Account> optionalAccount = accountRepository.findById(form.getAccountId());

            if (optionalAccount.isPresent()) {
                Account account = optionalAccount.get();
                certificateRequest.setRequestedBy(raUser.getUsername());
                certificateRequest.setAccount(account);
                certificateRequest = requestRepository.save(certificateRequest);

                //Allow automatic issuance
                if(account.isAllowAutomaticManualCertificateIssuance()){
                    certificateRequest.setStatus("approved");
                    processApprovedCertRequest(certificateRequest);

                    requestRepository.save(certificateRequest);
                }

                return certificateRequest.getId();
            } else {
                throw new RAObjectNotFoundException(Account.class, form.getAccountId());
            }
        } else {
            throw new InvalidFormException(form);
        }
    }

    private String buildFqdn(CertificateRequestForm form) throws Exception {
        String fqdn = form.getPrimaryDnsHostname();
        String domain = null;
        Long domainId = form.getPrimaryDnsDomainId();
        if(domainId != null){
            List<DomainInfo> domainInfos = domainService.optionsForAccount(form.getAccountId());
            Optional<DomainInfo> optionalDomainInfo = domainInfos.stream()
                    .filter(i -> i.getId().equals(domainId))
                    .findFirst();

            if(optionalDomainInfo.isPresent()){
                DomainInfo domainInfo = optionalDomainInfo.get();
                domain = domainInfo.getFullDomainName();
            }else{
                throw new Exception("Account not allowed to use domain");
            }
        }

        if(domain != null){
            fqdn += "."+domain;
        }

        return fqdn;
    }

    @GetMapping("/decision/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public CertificateRequest reviewRequestGet(@PathVariable Long id) throws RAObjectNotFoundException {
        Optional<CertificateRequest> optionalCertificateRequest = requestRepository.findById(id);

        if (optionalCertificateRequest.isPresent()) {
            CertificateRequest certificateRequest = optionalCertificateRequest.get();
            Hibernate.initialize(certificateRequest.getRequestedDnsNames());
            return certificateRequest;
        } else {
            throw new RAObjectNotFoundException(CertificateRequest.class, id);
        }
    }

    @PostMapping("/decision")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public CertificateRequest reviewRequest(@Valid @RequestBody CertificateRequestDecisionForm form) throws RAException {
        Optional<CertificateRequest> optionalCertificateRequest = requestRepository.findById(form.getRequestId());
        if (optionalCertificateRequest.isPresent()) {
            CertificateRequest request = optionalCertificateRequest.get();

            if ("approved".equals(form.getStatus())) {
                try {
                    Hibernate.initialize(request.getRequestedDnsNames());
                    processApprovedCertRequest(request);
                } catch (Exception e) {
                    log.error("Could not process approve CSR", e);
                    throw new RAException("Could not process approve CSR", e);
                }
            } else {
                request.setStatus(form.getStatus());
            }

            request = requestRepository.findById(request.getId()).get();
            return request;
        } else {
            throw new RAObjectNotFoundException(form);
        }
    }

    @GetMapping("/myRequests")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<CertificateRequest> myRequests(@AuthenticationPrincipal UserDetails raUser) {
        List<CertificateRequest> requests = requestRepository.findAllByRequestedByEquals(raUser.getUsername());

        requests.forEach(r -> Hibernate.initialize(r.getRequestedDnsNames()));

        return requests;
    }

    private void processApprovedCertRequest(CertificateRequest request) throws Exception {
        SubjectAltNames sans = new SubjectAltNames();
        sans.addValues(SubjectAltNames.SubjAltNameType.DNS, request.getRequestedDnsNamesAsSet());

        RACertificateIssueRequest raCertificateIssueRequest = new RACertificateIssueRequest(request.getAccount().getKeyIdentifier(),
                request.getCsr(), request.getPrimaryDnsName(), String.join(",", request.getRequestedDnsNames()), request.getCertAuthorityName(), "manual");
        raCertificateIssueRequest.setExistingCertificateRequestId(request.getId());

        CertIssuanceTransaction certIssuanceTransaction =
                new CertIssuanceTransaction(certAuthorityStore.getLoadedCertAuthority(
                raCertificateIssueRequest.getCertAuthorityName()), context);

        //todo broken for manual when custom SAN with no server entry submitted
        X509Certificate certificate = certIssuanceTransaction.processIssueCertificate(raCertificateIssueRequest, request.getAccount());
    }

}
