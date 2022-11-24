package com.winllc.pki.ra.service;

import com.winllc.acme.common.client.ca.LoadedCertAuthorityStore;
import com.winllc.pki.ra.beans.form.CertificateRequestDecisionForm;
import com.winllc.pki.ra.beans.form.CertificateValidationForm;
import com.winllc.acme.common.domain.RevocationRequest;
import com.winllc.pki.ra.beans.form.RevocationRequestForm;
import com.winllc.pki.ra.beans.search.GridFilterModel;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.RevocationRequestRepository;
import com.winllc.pki.ra.service.transaction.CertRevocationTransaction;
import com.winllc.ra.integration.ca.CertAuthority;
import com.winllc.ra.integration.ca.CertSearchParam;
import com.winllc.ra.integration.ca.CertSearchParams;
import com.winllc.ra.integration.ca.CertificateDetails;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.naming.InvalidNameException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.security.auth.x500.X500Principal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/revocationRequest")
public class RevocationRequestService extends UpdatedDataPagedService<RevocationRequest,
        RevocationRequestForm, RevocationRequestRepository> {

    private static final Logger log = LogManager.getLogger(RevocationRequestService.class);

    private final RevocationRequestRepository revocationRequestRepository;
    private final LoadedCertAuthorityStore certAuthorityStore;
    private final ApplicationContext applicationContext;

    public RevocationRequestService(RevocationRequestRepository revocationRequestRepository,
                                    LoadedCertAuthorityStore certAuthorityStore,
                                    ApplicationContext applicationContext) {
        super(applicationContext, RevocationRequest.class, revocationRequestRepository);
        this.revocationRequestRepository = revocationRequestRepository;
        this.certAuthorityStore = certAuthorityStore;
        this.applicationContext = applicationContext;
    }

    public RevocationRequest save(RevocationRequest request){
        return revocationRequestRepository.save(request);
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
            throws Exception {

        Optional<RevocationRequest> optionalRequest = revocationRequestRepository.findById(form.getRequestId());
        if(optionalRequest.isPresent()){
            RevocationRequest request = optionalRequest.get();

            switch (form.getStatus()){
                case "approve":
                    if(approveRevoke(request)){
                        request.setStatus("revoked");
                        request.setDecisionMadeBy(authentication.getName());
                        request.setStatusUpdatedOn(ZonedDateTime.now());
                    }
                    break;
                case "reject":
                    request.setStatus("rejected");
                    request.setDecisionMadeBy(authentication.getName());
                    request.setStatusUpdatedOn(ZonedDateTime.now());
                    break;
            }

            revocationRequestRepository.save(request);
        }else{
            throw new RAObjectNotFoundException(RevocationRequest.class, form.getRequestId());
        }

    }

    private boolean approveRevoke(RevocationRequest request) throws Exception {
        Optional<CertAuthority> optionalCa
                = certAuthorityStore.getLoadedCertAuthorityByIssuerDN(new X500Principal(request.getIssuerDn()));

        if(optionalCa.isPresent()){
            CertAuthority certAuthority = optionalCa.get();
            CertRevocationTransaction revocationTransaction = new CertRevocationTransaction(certAuthority, applicationContext);
            boolean revoked = revocationTransaction.processRevokeCertificate(request);

            log.debug("Was revoked: "+revoked);
            return revoked;
        }else{
            throw new RAObjectNotFoundException(CertAuthority.class, request.getIssuerDn());
        }
    }


    @Override
    protected void postSave(RevocationRequest entity, RevocationRequestForm form) {

    }

    @Override
    protected RevocationRequestForm entityToForm(RevocationRequest entity, Authentication authentication) {
        return new RevocationRequestForm(entity);
    }

    @Override
    protected RevocationRequest formToEntity(RevocationRequestForm form, Map<String, String> params, Authentication authentication) throws Exception {
        return null;
    }

    @Override
    protected RevocationRequest combine(RevocationRequest original, RevocationRequest updated, Authentication authentication) throws Exception {
        return null;
    }

    @Override
    public List<Predicate> buildFilter(Map<String, String> allRequestParams, GridFilterModel filterModel, Root<RevocationRequest> root, CriteriaQuery<?> query, CriteriaBuilder cb, Authentication authentication) {
        return null;
    }

}
