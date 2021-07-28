package com.winllc.pki.ra.service;

import com.winllc.acme.common.CertSearchParam;
import com.winllc.acme.common.CertSearchParams;
import com.winllc.acme.common.CertificateDetails;
import com.winllc.acme.common.ca.CachedCertificate;
import com.winllc.acme.common.ca.CertAuthority;
import com.winllc.acme.common.ca.LoadedCertAuthorityStore;
import com.winllc.acme.common.cache.CachedCertificateService;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.beans.info.AccountInfo;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.CachedCertificateRepository;
import com.winllc.acme.common.repository.CertificateRequestRepository;
import com.winllc.acme.common.repository.RevocationRequestRepository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchService {

    @Autowired
    private LoadedCertAuthorityStore certAuthorityStore;
    @Autowired
    private CertificateRequestRepository certificateRequestRepository;
    @Autowired
    private CachedCertificateRepository cachedCertificateRepository;
    @Autowired
    private RevocationRequestRepository revocationRequestRepository;
    @Autowired
    private CachedCertificateService cachedCertificateService;

    @GetMapping("/certificates")
    public List<CertificateDetails> searchCertificates(@RequestParam String search){

        List<CertificateDetails> details = new ArrayList<>();
        if(StringUtils.isNotEmpty(search) && search.length() > 2) {

            CertSearchParam param = CertSearchParam.createNew();
            param.setRelation(CertSearchParams.CertSearchParamRelation.CONTAINS);
            param.setField(CertSearchParams.CertField.SUBJECT);
            param.setValue(search);

            List<com.winllc.acme.common.ca.CachedCertificate> search1 = cachedCertificateService.search(param);

            return search1.stream()
                    .sorted()
                    .map(c -> cachedToDetails(c))
                    .collect(Collectors.toList());
        }
        return details;
    }

    @PostMapping("/certificates/advanced")
    public List<CertificateDetails> advancedSearchCertificates(@RequestBody CertSearchParam param){

        List<com.winllc.acme.common.ca.CachedCertificate> search1 = cachedCertificateService.search(param);

        return search1.stream()
                .map(c -> cachedToDetails(c))
                .collect(Collectors.toList());
    }

    private CertificateDetails cachedToDetails(com.winllc.acme.common.ca.CachedCertificate cachedCertificate){
        CertificateDetails details = new CertificateDetails();
        details.setValidFrom(ZonedDateTime.from(cachedCertificate.getValidFrom().toInstant().atZone(ZoneId.systemDefault())));
        details.setValidTo(ZonedDateTime.from(cachedCertificate.getValidTo().toInstant().atZone(ZoneId.systemDefault())));
        details.setIssuer(cachedCertificate.getIssuer());
        details.setSerial(cachedCertificate.getSerial().toString());
        details.setSubject(cachedCertificate.getDn());
        details.setCaName(cachedCertificate.getCaName());
        details.setStatus(cachedCertificate.getStatus());

        return details;
    }

    @Transactional
    @GetMapping("/certificate/details")
    public FullCertificateDetails getCertDetails(@RequestParam String caName,
                                                 @RequestParam String serial) throws Exception {
        CertAuthority certAuthority = certAuthorityStore.getLoadedCertAuthority(caName);

        //Optional<CachedCertificate> distinctByIssuerAndSerial = cachedCertificateRepository.findDistinctByIssuerAndSerial(
        //        certAuthority.getIssuerName().toString(), Long.getLong(serial));

        CertSearchParam serialSearch = CertSearchParam.createNew()
                .field(CertSearchParams.CertField.SERIAL)
                .relation(CertSearchParams.CertSearchParamRelation.EQUALS)
                .value(serial);

        CertSearchParam caSearch = CertSearchParam.createNew()
                .field(CertSearchParams.CertField.ISSUER)
                .relation(CertSearchParams.CertSearchParamRelation.EQUALS)
                .value(certAuthority.getIssuerName().toString());

        CertSearchParam andParam = CertSearchParam.createNew()
                .relation(CertSearchParams.CertSearchParamRelation.AND)
                .addSearchParam(serialSearch)
                .addSearchParam(caSearch);

        List<CachedCertificate> search = cachedCertificateService.search(andParam);

        if(CollectionUtils.isNotEmpty(search)){
            CachedCertificate cachedCertificate = search.get(0);
            X509Certificate certificate = CertUtil.base64ToCert(cachedCertificate.getBase64Certificate());
            FullCertificateDetails fullCertificateDetails =
                    new FullCertificateDetails(new CertificateDetails(certificate, cachedCertificate.getStatus()));

            return fullCertificateDetails;
        }else{
            throw new RAObjectNotFoundException(Certificate.class, serial);
        }
        /*
        X509Certificate certificate = certAuthority.getCertificateBySerial(serial);
        if(certificate != null){
            String issuerDn = certificate.getIssuerDN().getName();
            CertificateStatus status = certAuthority.getCertificateStatus(serial);
            FullCertificateDetails fullCertificateDetails =
                    new FullCertificateDetails(new CertificateDetails(certificate, status.name()));

            int serialNum = CertUtil.serialToInt(serial);

            Optional<CertificateRequest> optionalRequest = certificateRequestRepository.findDistinctByIssuedCertificateSerialAndCertAuthorityName(
                    Integer.toString(serialNum), caName);

            if(optionalRequest.isPresent()){
                CertificateRequest certificateRequest = optionalRequest.get();
                AccountInfo accountInfo =
                        new AccountInfo(certificateRequest.getAccount(), false);
                fullCertificateDetails.accountInfo = accountInfo;
            }

            Optional<RevocationRequest> optionalRevocationRequest = revocationRequestRepository.findDistinctByIssuerDnAndSerial(
                    issuerDn, serial);

            if(optionalRevocationRequest.isPresent()){
                RevocationRequest request = optionalRevocationRequest.get();
                if(request.getStatus().contentEquals("new")){
                    fullCertificateDetails.revocationPending = true;
                }
            }

            return fullCertificateDetails;
        }else{
            throw new RAObjectNotFoundException(Certificate.class, serial);
        }

         */
    }

    public static class FullCertificateDetails {
        private boolean revocationPending = false;
        private CertificateDetails details;
        private AccountInfo accountInfo;

        public boolean isRevocationPending() {
            return revocationPending;
        }

        public void setRevocationPending(boolean revocationPending) {
            this.revocationPending = revocationPending;
        }

        public FullCertificateDetails(CertificateDetails details) {
            this.details = details;
        }

        public CertificateDetails getDetails() {
            return details;
        }

        public void setDetails(CertificateDetails details) {
            this.details = details;
        }

        public AccountInfo getAccountInfo() {
            return accountInfo;
        }

        public void setAccountInfo(AccountInfo accountInfo) {
            this.accountInfo = accountInfo;
        }
    }
}
