package com.winllc.pki.ra.service;

import com.winllc.acme.common.CertSearchParam;
import com.winllc.acme.common.CertSearchParams;
import com.winllc.acme.common.CertificateDetails;
import com.winllc.pki.ra.beans.metrics.ChartMetrics;
import com.winllc.pki.ra.ca.CertAuthority;
import com.winllc.pki.ra.constants.AuditRecordType;
import com.winllc.pki.ra.domain.AuditRecord;
import com.winllc.pki.ra.repository.AuditRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/metrics")
public class MetricsService {

    @Autowired
    private CertAuthorityConnectionService certAuthorityConnectionService;
    @Autowired
    private AuditRecordRepository auditRecordRepository;

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

    @GetMapping("/mainChartMetrics")
    public ResponseEntity<?> generateMainChartMetrics(){
        ChartMetrics mainChartMetrics = ChartMetrics.buildForRange(LocalDate.now().minusDays(14), LocalDate.now());

        List<AuditRecord> issued = auditRecordRepository.findAllByTypeEquals(AuditRecordType.CERTIFICATE_ISSUED);
        List<AuditRecord> revoked = auditRecordRepository.findAllByTypeEquals(AuditRecordType.CERTIFICATE_REVOKED);

        Map<LocalDate, Integer> issuedMap = recordListToCountMap(issued);
        Map<LocalDate, Integer> revokedMap = recordListToCountMap(revoked);

        mainChartMetrics.addDataset(ChartMetrics.Dataset.DatasetType.SUCCESS, "Issued", issuedMap);
        mainChartMetrics.addDataset(ChartMetrics.Dataset.DatasetType.DANGER, "Revoked", revokedMap);

        return ResponseEntity.ok(mainChartMetrics);
    }

    private Map<LocalDate, Integer> recordListToCountMap(List<AuditRecord> records){
        Map<LocalDate, List<AuditRecord>> collect =
                records.stream()
                        .collect(Collectors.groupingBy(a -> {
                            LocalDateTime ldt = a.getTimestamp().toLocalDateTime();
                            return ldt.toLocalDate();
                        }));

        Map<LocalDate, Integer> datasetMap = collect.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().size()));

        return datasetMap;
    }

}
