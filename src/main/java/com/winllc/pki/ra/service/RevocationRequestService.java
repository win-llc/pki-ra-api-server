package com.winllc.pki.ra.service;

import com.winllc.acme.common.CertSearchParam;
import com.winllc.acme.common.CertSearchParams;
import com.winllc.acme.common.CertificateDetails;
import com.winllc.acme.common.ca.CertAuthority;
import com.winllc.acme.common.contants.RevocationReason;
import com.winllc.pki.ra.beans.form.CertificateRequestDecisionForm;
import com.winllc.pki.ra.beans.form.CertificateValidationForm;
import com.winllc.pki.ra.ca.LoadedCertAuthorityStore;
import com.winllc.pki.ra.domain.CachedCertificate;
import com.winllc.pki.ra.domain.CertificateRequest;
import com.winllc.pki.ra.domain.RevocationRequest;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.CachedCertificateRepository;
import com.winllc.pki.ra.repository.RevocationRequestRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.x500.X500Principal;
import java.security.Principal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/revocationRequest")
public class RevocationRequestService {

    private static final Logger log = LogManager.getLogger(RevocationRequestService.class);

    private final RevocationRequestRepository revocationRequestRepository;
    private final LoadedCertAuthorityStore certAuthorityStore;
    @Autowired
    private CachedCertificateRepository cachedCertificateRepository;

    public RevocationRequestService(RevocationRequestRepository revocationRequestRepository,
                                    LoadedCertAuthorityStore certAuthorityStore) {
        this.revocationRequestRepository = revocationRequestRepository;
        this.certAuthorityStore = certAuthorityStore;
    }


    @GetMapping("/all")
    public List<RevocationRequest> request(){

        return revocationRequestRepository.findAll();
    }

    @GetMapping("/status/{status}")
    public List<RevocationRequest> status(@PathVariable String status){

        return revocationRequestRepository.findAllByStatus(status);
    }

    @GetMapping("/byId/{id}")
    public RevocationRequest byId(@PathVariable Long id) throws RAObjectNotFoundException {

        Optional<RevocationRequest> optionalRequest = revocationRequestRepository.findById(id);
        if(optionalRequest.isPresent()){
            return optionalRequest.get();
        }else{
            throw new RAObjectNotFoundException(RevocationRequest.class, id);
        }
    }

    @PostMapping("/request")
    public void request(@RequestBody CertificateValidationForm form,
                        Authentication authentication) throws InvalidNameException {

        log.info("Debug");

        Optional<CertAuthority> loadedCertAuthorityOptional
                = certAuthorityStore.getLoadedCertAuthorityByIssuerDN(new X500Principal(form.getIssuerDn()));

        if(loadedCertAuthorityOptional.isPresent()){
            CertAuthority ca = loadedCertAuthorityOptional.get();

            CertSearchParam param = CertSearchParam.createNew();
            param.setRelation(CertSearchParams.CertSearchParamRelation.EQUALS);
            param.setField(CertSearchParams.CertField.SERIAL);
            param.setValue(form.getSerial());

            List<CertificateDetails> results = ca.search(param);
            log.info("results: "+results.size());

            if(results.size() > 0){
                CertificateDetails details = results.get(0);
                RevocationRequest request = new RevocationRequest(authentication.getName());
                request.setSerial(form.getSerial());
                request.setIssuerDn(form.getIssuerDn());
                request.setSubjectDn(details.getSubject());

                revocationRequestRepository.save(request);
            }
        }
    }

    @PostMapping("/decision")
    public void approve(@RequestBody CertificateRequestDecisionForm form, Authentication authentication)
            throws InvalidNameException, RAObjectNotFoundException {

        Optional<RevocationRequest> optionalRequest = revocationRequestRepository.findById(form.getRequestId());
        if(optionalRequest.isPresent()){
            RevocationRequest request = optionalRequest.get();

            switch (form.getStatus()){
                case "approve":
                    if(approveRevoke(request)){
                        request.setStatus("revoked");
                        request.setDecisionMadeBy(authentication.getName());
                        request.setStatusUpdatedOn(Timestamp.valueOf(LocalDateTime.now()));
                    }
                    break;
                case "reject":
                    request.setStatus("rejected");
                    request.setDecisionMadeBy(authentication.getName());
                    request.setStatusUpdatedOn(Timestamp.valueOf(LocalDateTime.now()));
                    break;
            }

            revocationRequestRepository.save(request);
        }else{
            throw new RAObjectNotFoundException(RevocationRequest.class, form.getRequestId());
        }

    }

    private boolean approveRevoke(RevocationRequest request) throws InvalidNameException, RAObjectNotFoundException {
        Optional<CertAuthority> optionalCa
                = certAuthorityStore.getLoadedCertAuthorityByIssuerDN(new X500Principal(request.getIssuerDn()));

        if(optionalCa.isPresent()){
            CertAuthority ca = optionalCa.get();

            boolean revoked = false;
            try {
                revoked = ca.revokeCertificate(request.getSerial(), RevocationReason.UNSPECIFIED.getCode());

                if(revoked) {
                    Optional<CachedCertificate> optionalCached = cachedCertificateRepository.findDistinctByIssuerAndSerial(
                            request.getIssuerDn(), Long.parseLong(request.getSerial()));

                    if (optionalCached.isPresent()) {
                        CachedCertificate cachedCertificate = optionalCached.get();
                        cachedCertificate.setStatus("REVOKED");
                        cachedCertificateRepository.save(cachedCertificate);
                    }
                }
            } catch (Exception e) {
                log.error("Could not revoke: "+request);
            }

            log.debug("Was revoked: "+revoked);
            return revoked;
        }else{
            throw new RAObjectNotFoundException(CertAuthority.class, request.getIssuerDn());
        }
    }
}
