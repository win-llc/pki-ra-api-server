package com.winllc.pki.ra.service;

import com.winllc.acme.common.ra.RACertificateIssueRequest;
import com.winllc.acme.common.SubjectAltNames;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.beans.form.CertificateRequestDecisionForm;
import com.winllc.pki.ra.beans.form.CertificateRequestForm;
import com.winllc.pki.ra.beans.info.CertificateRequestInfo;
import com.winllc.pki.ra.beans.validator.CertRequestFormValidator;
import com.winllc.pki.ra.beans.validator.ValidationResponse;
import com.winllc.pki.ra.constants.AuditRecordType;
import com.winllc.pki.ra.domain.*;
import com.winllc.pki.ra.exception.InvalidFormException;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.*;
import com.winllc.pki.ra.security.RAUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/certificateRequest")
public class CertificateRequestService {

    private static final Logger log = LogManager.getLogger(CertificateRequestService.class);

    @Autowired
    private CertificateRequestRepository requestRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AuditRecordRepository auditRecordRepository;
    @Autowired
    private CertAuthorityConnectionService certAuthorityConnectionService;
    @Autowired
    private ServerEntryRepository serverEntryRepository;
    @Autowired
    private CertRequestFormValidator formValidator;

    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public List<CertificateRequest> getAll() {
        List<CertificateRequest> requests = requestRepository.findAll();
        return requests;
    }

    @GetMapping("/allWithStatus/{status}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<CertificateRequest> getAllWithStatus(@PathVariable String status) {
        List<CertificateRequest> requests = requestRepository.findAllByStatusEquals(status);

        requests.forEach(r -> Hibernate.initialize(r.getRequestedDnsNames()));

        return requests;
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

    @PostMapping("/submit")
    @ResponseStatus(HttpStatus.CREATED)
    public Long submitRequest(@Valid @RequestBody CertificateRequestForm form, @AuthenticationPrincipal UserDetails raUser)
            throws InvalidFormException, RAObjectNotFoundException {
        ValidationResponse validationResponse = formValidator.validate(form, false);
        if (validationResponse.isValid()) {
            CertificateRequest certificateRequest = CertificateRequest.build();
            certificateRequest.setCsr(form.getCsr());
            certificateRequest.setCertAuthorityName(form.getCertAuthorityName());
            certificateRequest.setRequestedDnsNames(form.getRequestedDnsNames().stream()
                    .map(d -> d.getValue())
                    .collect(Collectors.toList()));

            Optional<Account> optionalAccount = accountRepository.findById(form.getAccountId());
            Optional<User> optionalUser = userRepository.findOneByUsername(raUser.getUsername());

            if (optionalUser.isPresent() && optionalAccount.isPresent()) {
                User user = optionalUser.get();
                Account account = optionalAccount.get();
                certificateRequest.setRequestedBy(user);
                certificateRequest.setAccount(account);
                certificateRequest = requestRepository.save(certificateRequest);
                return certificateRequest.getId();
            } else {
                throw new RAObjectNotFoundException(User.class, raUser.getUsername());
            }
        } else {
            throw new InvalidFormException(form);
        }
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

            request = requestRepository.save(request);
            return request;
        } else {
            throw new RAObjectNotFoundException(form);
        }
    }

    @GetMapping("/myRequests")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<CertificateRequest> myRequests(@AuthenticationPrincipal UserDetails raUser) throws RAObjectNotFoundException {
        Optional<User> optionalUser = userRepository.findOneByUsername(raUser.getUsername());
        if (optionalUser.isPresent()) {
            User currentUser = optionalUser.get();

            List<CertificateRequest> requests = requestRepository.findAllByRequestedByEquals(currentUser);

            requests.forEach(r -> Hibernate.initialize(r.getRequestedDnsNames()));

            return requests;
        } else {
            throw new RAObjectNotFoundException(User.class, raUser.getUsername());
        }
    }


    private void processApprovedCertRequest(CertificateRequest request) throws Exception {
        //todo route this through CertAuthorityConnectionService

        SubjectAltNames sans = new SubjectAltNames();
        sans.addValues(SubjectAltNames.SubjAltNameType.DNS, request.getRequestedDnsNames());

        RACertificateIssueRequest raCertificateIssueRequest = new RACertificateIssueRequest(request.getAccount().getKeyIdentifier(),
                request.getCsr(), String.join(",", request.getRequestedDnsNames()), request.getCertAuthorityName());

        X509Certificate issuedCertificate = certAuthorityConnectionService.processIssueCertificate(raCertificateIssueRequest);

        //check if any server entries with same subject exist, if so, associate this request
        //todo
        String issuedFqdn = getFqdnFromCert(issuedCertificate);

        Optional<ServerEntry> serverEntryOptional = serverEntryRepository.findDistinctByFqdnEquals(issuedFqdn);
        if (serverEntryOptional.isPresent()) {
            ServerEntry serverEntry = serverEntryOptional.get();
            serverEntry.getCertificateRequests().add(request);
            serverEntryRepository.save(serverEntry);
        }

        AuditRecord record = AuditRecord.buildNew(AuditRecordType.CERTIFICATE_ISSUED);
        record.setAccountKid(raCertificateIssueRequest.getAccountKid());
        record.setSource("manual");
        auditRecordRepository.save(record);

        String certPem = CertUtil.formatCrtFileContents(issuedCertificate);
        request.setIssuedCertificate(certPem);
        request.setReviewedOn(Timestamp.valueOf(LocalDateTime.now()));
        request.setStatus("issued");
    }

    private static String getFqdnFromCert(X509Certificate issuedCertificate){
        String certificateDn = issuedCertificate.getSubjectDN().getName();
        String issuedFqdn = certificateDn.toLowerCase();

        if(certificateDn.contains(",")){
            issuedFqdn = issuedFqdn.substring(0, certificateDn.indexOf(",")).replace("cn=", "");
        }

        issuedFqdn = issuedFqdn.replace("cn=", "");

        return issuedFqdn;
    }

}
