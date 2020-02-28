package com.winllc.pki.ra.service;

import com.winllc.acme.common.ra.RACertificateIssueRequest;
import com.winllc.acme.common.SubjectAltNames;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.beans.form.CertificateRequestDecisionForm;
import com.winllc.pki.ra.beans.form.CertificateRequestForm;
import com.winllc.pki.ra.beans.info.CertificateRequestInfo;
import com.winllc.pki.ra.beans.validator.CertRequestFormValidator;
import com.winllc.pki.ra.constants.AuditRecordType;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.AuditRecord;
import com.winllc.pki.ra.domain.CertificateRequest;
import com.winllc.pki.ra.domain.User;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.AuditRecordRepository;
import com.winllc.pki.ra.repository.CertificateRequestRepository;
import com.winllc.pki.ra.repository.UserRepository;
import com.winllc.pki.ra.security.RAUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private CertRequestFormValidator formValidator;

    @GetMapping("/all")
    public ResponseEntity<?> getAll() {
        List<CertificateRequest> requests = requestRepository.findAll();
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/allWithStatus/{status}")
    @Transactional
    public ResponseEntity<?> getAllWithStatus(@PathVariable String status) {
        List<CertificateRequest> requests = requestRepository.findAllByStatusEquals(status);

        requests.forEach(r -> Hibernate.initialize(r.getRequestedDnsNames()));

        return ResponseEntity.ok(requests);
    }

    @GetMapping("/byId/{id}")
    public ResponseEntity<?> byId(@PathVariable Long id) {
        Optional<CertificateRequest> byId = requestRepository.findById(id);

        if (byId.isPresent()) {
            return ResponseEntity.ok(new CertificateRequestInfo(byId.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/byIdFull/{id}")
    @Transactional
    public ResponseEntity<?> byIdFull(@PathVariable Long id) {
        Optional<CertificateRequest> byId = requestRepository.findById(id);

        if (byId.isPresent()) {
            CertificateRequest certificateRequest = byId.get();
            Hibernate.initialize(certificateRequest.getRequestedDnsNames());
            return ResponseEntity.ok(certificateRequest);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitRequest(@Valid @RequestBody CertificateRequestForm form, @AuthenticationPrincipal RAUser raUser) {
        boolean valid = formValidator.validate(form, false);
        if (valid) {
            CertificateRequest certificateRequest = CertificateRequest.build();
            certificateRequest.setCsr(form.getCsr());
            certificateRequest.setCertAuthorityName(form.getCertAuthorityName());
            certificateRequest.setRequestedDnsNames(form.getRequestedDnsNames().stream()
                    .map(d -> d.getValue())
                    .collect(Collectors.toList()));

            Optional<Account> optionalAccount = accountRepository.findById(form.getAccountId());
            Optional<User> optionalUser = userRepository.findOneByUsername(raUser.getUsername());

            if (optionalUser.isPresent() &&  optionalAccount.isPresent()) {
                User user = optionalUser.get();
                Account account = optionalAccount.get();
                certificateRequest.setRequestedBy(user);
                certificateRequest.setAccount(account);
                certificateRequest = requestRepository.save(certificateRequest);
                return ResponseEntity.ok(certificateRequest.getId());
            } else {
                return ResponseEntity.notFound().build();
            }
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/decision/{id}")
    @Transactional
    public ResponseEntity<?> reviewRequestGet(@PathVariable Long id) {
        Optional<CertificateRequest> optionalCertificateRequest = requestRepository.findById(id);

        if (optionalCertificateRequest.isPresent()) {
            CertificateRequest certificateRequest = optionalCertificateRequest.get();
            Hibernate.initialize(certificateRequest.getRequestedDnsNames());
            return ResponseEntity.ok(certificateRequest);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/decision")
    @Transactional
    public ResponseEntity<?> reviewRequest(@Valid @RequestBody CertificateRequestDecisionForm form) {
        Optional<CertificateRequest> optionalCertificateRequest = requestRepository.findById(form.getRequestId());
        if (optionalCertificateRequest.isPresent()) {
            CertificateRequest request = optionalCertificateRequest.get();

            if ("approved".equals(form.getStatus())) {
                try {
                    Hibernate.initialize(request.getRequestedDnsNames());
                    processApprovedCertRequest(request);
                } catch (Exception e) {
                    log.error("Could not process approve CSR", e);
                    return ResponseEntity.status(500).build();
                }
            } else {
                request.setStatus(form.getStatus());
            }

            requestRepository.save(request);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/myRequests")
    @Transactional
    public ResponseEntity<?> myRequests(@AuthenticationPrincipal RAUser raUser) {
        Optional<User> optionalUser = userRepository.findOneByUsername(raUser.getUsername());
        if (optionalUser.isPresent()) {
            User currentUser = optionalUser.get();

            List<CertificateRequest> requests = requestRepository.findAllByRequestedByEquals(currentUser);

            requests.forEach(r -> Hibernate.initialize(r.getRequestedDnsNames()));

            return ResponseEntity.ok(requests);
        } else {
            return ResponseEntity.status(403).build();
        }
    }


    private void processApprovedCertRequest(CertificateRequest request) throws Exception {
        //todo route this through CertAuthorityConnectionService

        SubjectAltNames sans = new SubjectAltNames();
        sans.addValues(SubjectAltNames.SubjAltNameType.DNS, request.getRequestedDnsNames());

        RACertificateIssueRequest raCertificateIssueRequest = new RACertificateIssueRequest(request.getAccount().getKeyIdentifier(),
                request.getCsr(), String.join(",", request.getRequestedDnsNames()), request.getCertAuthorityName());

        X509Certificate issuedCertificate = certAuthorityConnectionService.processIssueCertificate(raCertificateIssueRequest);

        AuditRecord record = AuditRecord.buildNew(AuditRecordType.CERTIFICATE_ISSUED);
        record.setAccountKid(raCertificateIssueRequest.getAccountKid());
        record.setSource("manual");
        auditRecordRepository.save(record);

        String certPem = CertUtil.convertToPem(issuedCertificate);
        request.setIssuedCertificate(certPem);
        request.setReviewedOn(Timestamp.valueOf(LocalDateTime.now()));
        request.setStatus("issued");
    }

}
