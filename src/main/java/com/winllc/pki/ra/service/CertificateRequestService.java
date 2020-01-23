package com.winllc.pki.ra.service;

import com.winllc.acme.common.SubjectAltNames;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.beans.form.CertificateRequestDecisionForm;
import com.winllc.pki.ra.beans.form.CertificateRequestForm;
import com.winllc.pki.ra.beans.info.CertificateRequestInfo;
import com.winllc.pki.ra.ca.CertAuthority;
import com.winllc.pki.ra.domain.CertificateRequest;
import com.winllc.pki.ra.domain.User;
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
    private CertAuthorityConnectionService certAuthorityConnectionService;

    @GetMapping("/all")
    public ResponseEntity<?> getAll(){
        List<CertificateRequest> requests = requestRepository.findAll();
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/allWithStatus/{status}")
    @Transactional
    public ResponseEntity<?> getAllWithStatus(@PathVariable String status){
        List<CertificateRequest> requests = requestRepository.findAllByStatusEquals(status);

        requests.forEach(r -> Hibernate.initialize(r.getRequestedDnsNames()));

        return ResponseEntity.ok(requests);
    }

    @GetMapping("/byId/{id}")
    public ResponseEntity<?> byId(@PathVariable Long id){
        Optional<CertificateRequest> byId = requestRepository.findById(id);

        if(byId.isPresent()){
            return ResponseEntity.ok(new CertificateRequestInfo(byId.get()));
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitRequest(@RequestBody CertificateRequestForm form, @AuthenticationPrincipal RAUser raUser){
        if(validateRequestForm(form)){
            CertificateRequest certificateRequest = CertificateRequest.build();
            certificateRequest.setCsr(form.getCsr());
            certificateRequest.setCertAuthorityName(form.getCertAuthorityName());
            certificateRequest.setRequestedDnsNames(form.getRequestedDnsNames().stream()
                    .map(d -> d.getValue())
                    .collect(Collectors.toList()));

            Optional<User> optionalUser = userRepository.findOneByUsername(raUser.getUsername());

            if(optionalUser.isPresent()) {
                User user = optionalUser.get();
                certificateRequest.setRequestedBy(user);
                certificateRequest = requestRepository.save(certificateRequest);
                return ResponseEntity.ok(certificateRequest.getId());
            }else{
                return ResponseEntity.notFound().build();
            }
        }else{
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/decision/{id}")
    @Transactional
    public ResponseEntity<?> reviewRequestGet(@PathVariable Long id){
        Optional<CertificateRequest> optionalCertificateRequest = requestRepository.findById(id);

        if(optionalCertificateRequest.isPresent()){
            CertificateRequest certificateRequest = optionalCertificateRequest.get();
            Hibernate.initialize(certificateRequest.getRequestedDnsNames());
            return ResponseEntity.ok(certificateRequest);
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/decision")
    @Transactional
    public ResponseEntity<?> reviewRequest(@RequestBody CertificateRequestDecisionForm form){
        Optional<CertificateRequest> optionalCertificateRequest = requestRepository.findById(form.getRequestId());
        if(optionalCertificateRequest.isPresent()){
            CertificateRequest request = optionalCertificateRequest.get();

            if ("approved".equals(form.getStatus())) {
                try {
                    Hibernate.initialize(request.getRequestedDnsNames());
                    processApprovedCertRequest(request);
                } catch (Exception e) {
                    log.error("Could not process approve CSR", e);
                    return ResponseEntity.status(500).build();
                }
            }else{
                request.setStatus(form.getStatus());
            }

            requestRepository.save(request);
            return ResponseEntity.ok().build();
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/myRequests")
    @Transactional
    public ResponseEntity<?> myRequests(@AuthenticationPrincipal RAUser raUser){
        Optional<User> optionalUser = userRepository.findOneByUsername(raUser.getUsername());
        if(optionalUser.isPresent()) {
            User currentUser = optionalUser.get();

            List<CertificateRequest> requests = requestRepository.findAllByRequestedByEquals(currentUser);

            requests.forEach(r -> Hibernate.initialize(r.getRequestedDnsNames()));

            return ResponseEntity.ok(requests);
        }else{
            return ResponseEntity.status(403).build();
        }
    }


    private void processApprovedCertRequest(CertificateRequest request) throws Exception {
        Optional<CertAuthority> optionalCertAuthority = certAuthorityConnectionService.getCertAuthorityByName(request.getCertAuthorityName());

        if(optionalCertAuthority.isPresent()){
            CertAuthority ca = optionalCertAuthority.get();

            SubjectAltNames sans = new SubjectAltNames();
            sans.addValues(SubjectAltNames.SubjAltNameType.DNS, request.getRequestedDnsNames());

            X509Certificate issuedCertificate = ca.issueCertificate(request.getCsr(), sans);

            String certPem = CertUtil.convertToPem(issuedCertificate);
            request.setIssuedCertificate(certPem);
            request.setReviewedOn(Timestamp.valueOf(LocalDateTime.now()));
            request.setStatus("issued");
        }else{
            throw new Exception("Could not find Cert Authority: "+request.getCertAuthorityName());
        }

    }

    private boolean validateRequestForm(CertificateRequestForm form){
        boolean valid = false;
        try {
            CertUtil.csrBase64ToPKC10Object(form.getCsr());
            valid = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return valid;
    }
}
