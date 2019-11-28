package com.winllc.pki.ra.service;

import com.winllc.acme.common.CertSearchParam;
import com.winllc.acme.common.CertSearchParams;
import com.winllc.acme.common.CertificateDetails;
import com.winllc.pki.ra.ca.CertAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricsService {

    @Autowired
    private CertAuthorityConnectionService certAuthorityConnectionService;

    @GetMapping("/totalAccounts")
    public ResponseEntity<?> getTotalAccounts(){
        //todo
        return null;
    }


    @GetMapping("/issuedCertificatesCount")
    public ResponseEntity<?> getIssuedCertificatesCount(){
        Map<String, Integer> issuedCertsTotalMap = new HashMap<>();

        CertSearchParam searchParam = new CertSearchParam(CertSearchParams.CertField.STATUS, "VALID", CertSearchParams.CertSearchParamRelation.EQUALS);

        for(CertAuthority ca : certAuthorityConnectionService.getAllCertAuthorities()){
            List<CertificateDetails> results = ca.search(searchParam);
            issuedCertsTotalMap.put(ca.getName(), results.size());
        }

        return ResponseEntity.ok(issuedCertsTotalMap);
    }
}
