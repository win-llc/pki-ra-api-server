package com.winllc.pki.ra.service;

import com.winllc.acme.common.CertSearchParam;
import com.winllc.acme.common.CertSearchParams;
import com.winllc.acme.common.CertificateDetails;
import com.winllc.pki.ra.beans.metrics.AuditMetricRequest;
import com.winllc.pki.ra.beans.metrics.AuditMetricResponse;
import com.winllc.pki.ra.beans.metrics.ChartMetrics;
import com.winllc.pki.ra.beans.metrics.DailyAuditMetric;
import com.winllc.pki.ra.ca.CertAuthority;
import com.winllc.pki.ra.ca.LoadedCertAuthorityStore;
import com.winllc.pki.ra.constants.AuditRecordType;
import com.winllc.pki.ra.domain.AuditRecord;
import com.winllc.pki.ra.repository.AuditRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/metrics")
public class MetricsService {

    private static final String dtfPattern = "dd/MM/yyyy";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dtfPattern);

    @Autowired
    private LoadedCertAuthorityStore certAuthorityStore;
    @Autowired
    private AuditRecordRepository auditRecordRepository;

    @GetMapping("/auditRecordTypes")
    public List<String> auditRecordTypes(){
        return Stream.of(AuditRecordType.values())
                .map(r -> r.name())
                .collect(Collectors.toList());
    }

    @PostMapping("/auditSearch")
    public AuditMetricResponse auditSearch(@RequestBody AuditMetricRequest auditMetricRequest){

        AuditRecordType type = AuditRecordType.valueOf(auditMetricRequest.getAuditRecordType());
        LocalDate fromDateTime = LocalDate.parse(auditMetricRequest.getDateFrom(), dtf);
        LocalDate toDateTime = LocalDate.parse(auditMetricRequest.getDateTo(), dtf);

        List<AuditRecord> allByTypeEqualsAndTimestampAfterAndTimestampBefore
                = auditRecordRepository.findAllByTypeEqualsAndTimestampAfterAndTimestampBefore(type,
                Timestamp.valueOf(fromDateTime.atStartOfDay()), Timestamp.valueOf(toDateTime.atStartOfDay()));

        Map<LocalDate, List<AuditRecord>> collect = allByTypeEqualsAndTimestampAfterAndTimestampBefore.stream()
                .collect(Collectors.groupingBy(a -> {
                    return a.getTimestamp().toLocalDateTime().toLocalDate();
                }));

        List<LocalDate> allDates = new LinkedList<>();
        LocalDate ldIterate = fromDateTime;
        while(ldIterate.isBefore(toDateTime) || ldIterate.equals(toDateTime)){
            allDates.add(ldIterate);
            ldIterate = ldIterate.plusDays(1);
        }

        List<DailyAuditMetric> auditMetricsDaily = allDates.stream()
                .map(d -> {
                    List<AuditRecord> records = new ArrayList<>();
                    if(collect.containsKey(d)){
                        records = collect.get(d);
                    }

                    DailyAuditMetric dailyAuditMetric = new DailyAuditMetric();
                    dailyAuditMetric.setDate(d.format(dtf));
                    dailyAuditMetric.setRecordsTotal(records.size());
                    if(auditMetricRequest.getReturnFullAuditRecords()) {
                        dailyAuditMetric.setAuditRecords(records);
                    }
                    return dailyAuditMetric;

                }).collect(Collectors.toList());

        AuditMetricResponse response = new AuditMetricResponse();
        response.setDateFormat(dtfPattern);
        response.setAuditMetrics(auditMetricsDaily);
        return response;
    }

    @GetMapping("/totalAccounts")
    public ResponseEntity<?> getTotalAccounts(){
        //todo
        return null;
    }

    @GetMapping("/issuedCertificatesCount")
    public ResponseEntity<?> getIssuedCertificatesCount(){
        Map<String, Integer> issuedCertsTotalMap = new HashMap<>();

        CertSearchParam searchParam = new CertSearchParam(CertSearchParams.CertField.STATUS, "VALID", CertSearchParams.CertSearchParamRelation.EQUALS);

        for(CertAuthority ca : certAuthorityStore.getAllCertAuthorities()){
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
