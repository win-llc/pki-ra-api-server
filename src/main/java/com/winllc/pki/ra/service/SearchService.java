package com.winllc.pki.ra.service;

import com.winllc.acme.common.CertSearchParam;
import com.winllc.acme.common.CertSearchParams;
import com.winllc.acme.common.CertificateDetails;
import com.winllc.acme.common.ca.CertAuthority;
import com.winllc.acme.common.contants.CertificateStatus;
import com.winllc.pki.ra.beans.info.AccountInfo;
import com.winllc.pki.ra.ca.LoadedCertAuthorityStore;
import com.winllc.pki.ra.domain.CachedCertificate;
import com.winllc.pki.ra.domain.CertificateRequest;
import com.winllc.pki.ra.domain.RevocationRequest;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.CachedCertificateRepository;
import com.winllc.pki.ra.repository.CertificateRequestRepository;
import com.winllc.pki.ra.repository.RevocationRequestRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @GetMapping("/certificates")
    public List<CertificateDetails> searchCertificates(@RequestParam String search){

        List<CertificateDetails> details = new ArrayList<>();
        if(StringUtils.isNotEmpty(search) && search.length() > 2) {

            List<CachedCertificate> found = cachedCertificateRepository.findAllByDnContainsIgnoreCase(search);
            return found.stream()
                    .sorted()
                    .map(c -> cachedToDetails(c))
                    .collect(Collectors.toList());
            /*
            CertSearchParam param = new CertSearchParam(CertSearchParams.CertField.SUBJECT,
                    search, CertSearchParams.CertSearchParamRelation.CONTAINS);

            Map<String, CertAuthority> loadedCertAuthorities = certAuthorityStore.getLoadedCertAuthorities();
            for (Map.Entry<String, CertAuthority> entry : loadedCertAuthorities.entrySet()) {
                List<CertificateDetails> results = entry.getValue().search(param);
                results.forEach(r -> {
                    r.setCaName(entry.getKey());
                });

                details.addAll(results);
            }

             */
        }
        return details;
    }

    private CertificateDetails cachedToDetails(CachedCertificate cachedCertificate){
        CertificateDetails details = new CertificateDetails();
        details.setValidFrom(cachedCertificate.getValidFrom().toLocalDateTime().atZone(ZoneId.systemDefault()));
        details.setValidTo(cachedCertificate.getValidTo().toInstant().atZone(ZoneId.systemDefault()));
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

        X509Certificate certificate = certAuthority.getCertificateBySerial(serial);
        if(certificate != null){
            String issuerDn = certificate.getIssuerDN().getName();
            CertificateStatus status = certAuthority.getCertificateStatus(serial);
            FullCertificateDetails fullCertificateDetails =
                    new FullCertificateDetails(new CertificateDetails(certificate, status.name()));

            int serialNum;
            if(serial.startsWith("0x")){
                serialNum = Integer.parseInt(serial.replace("0x", ""),16);
            }else{
                serialNum = Integer.parseInt(serial);
            }

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
