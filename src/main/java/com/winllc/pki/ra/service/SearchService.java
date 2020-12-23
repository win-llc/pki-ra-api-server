package com.winllc.pki.ra.service;

import com.winllc.acme.common.CertSearchParam;
import com.winllc.acme.common.CertSearchParams;
import com.winllc.acme.common.CertificateDetails;
import com.winllc.acme.common.ca.CertAuthority;
import com.winllc.pki.ra.beans.info.AccountInfo;
import com.winllc.pki.ra.ca.LoadedCertAuthorityStore;
import com.winllc.pki.ra.domain.CertificateRequest;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.CertificateRequestRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/search")
public class SearchService {

    @Autowired
    private LoadedCertAuthorityStore certAuthorityStore;
    @Autowired
    private CertificateRequestRepository certificateRequestRepository;

    @GetMapping("/certificates")
    public List<CertificateDetails> searchCertificates(@RequestParam String search){

        List<CertificateDetails> details = new ArrayList<>();
        if(StringUtils.isNotEmpty(search) && search.length() > 2) {
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
        }
        return details;
    }

    @Transactional
    @GetMapping("/certificate/details")
    public FullCertificateDetails getCertDetails(@RequestParam String caName,
                                                 @RequestParam String serial) throws Exception {
        CertAuthority certAuthority = certAuthorityStore.getLoadedCertAuthority(caName);

        X509Certificate certificate = certAuthority.getCertificateBySerial(serial);
        if(certificate != null){
            FullCertificateDetails fullCertificateDetails = new FullCertificateDetails();
            fullCertificateDetails.details = new CertificateDetails(certificate);

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

            return fullCertificateDetails;
        }else{
            throw new RAObjectNotFoundException(Certificate.class, serial);
        }
    }

    public static class FullCertificateDetails {
        private CertificateDetails details;
        private AccountInfo accountInfo;

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
