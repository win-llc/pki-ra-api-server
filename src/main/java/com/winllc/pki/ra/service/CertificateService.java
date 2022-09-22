package com.winllc.pki.ra.service;

import com.winllc.acme.common.ca.CachedCertificate;
import com.winllc.acme.common.cache.CachedCertificateService;
import com.winllc.acme.common.domain.CertificateRequest;
import com.winllc.acme.common.domain.ServerEntry;
import com.winllc.acme.common.repository.CertificateRequestRepository;
import com.winllc.acme.common.repository.ServerEntryRepository;
import com.winllc.ra.integration.ca.CertSearchParam;
import com.winllc.ra.integration.ca.CertSearchParams;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;
import java.time.LocalDate;
import java.sql.Date;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/certificates")
public class CertificateService {

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("mm/DD/yyyy");

    @Autowired
    private CachedCertificateService cachedCertificateService;
    private final CertificateRequestRepository certificateRequestRepository;
    private final ServerEntryRepository serverEntryRepository;

    public CertificateService(CertificateRequestRepository certificateRequestRepository,
                              ServerEntryRepository serverEntryRepository) {
        this.certificateRequestRepository = certificateRequestRepository;
        this.serverEntryRepository = serverEntryRepository;
    }

    @GetMapping("/getPaged")
    @Transactional
    public Page<CachedCertificate> getPaged(@RequestParam(required = false) String search,
                                            @RequestParam(defaultValue = "0") Integer page,
                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                            @RequestParam(defaultValue = "validTo") String sortBy,
                                            @RequestParam(defaultValue = "desc") String order,
                                            @RequestParam(defaultValue = "false") Boolean includeArchive,
                                            @RequestParam(defaultValue = "true") Boolean includeNonArchive,
                                            @RequestParam(defaultValue = "false") Boolean includeExpired,
                                            @RequestParam(defaultValue = "true") Boolean includeValid,
                                            @RequestParam(defaultValue = "") String notBefore,
                                            @RequestParam(defaultValue = "") String notAfter,
                                            @RequestParam(defaultValue = "-1") Integer expiresInDays) {

        FieldSortBuilder sortBuilder = new FieldSortBuilder(sortBy);
        if (order.equalsIgnoreCase("asc")) {
            sortBuilder.order(SortOrder.ASC);
        } else {
            sortBuilder.order(SortOrder.DESC);
        }

        Date notBeforeDate = null;
        Date notAfterDate = null;

        CertSearchParam andParam = CertSearchParam.createNew(CertSearchParams.CertSearchParamRelation.AND);

        if (StringUtils.isNotBlank(notBefore)) {
            LocalDate ld = LocalDate.parse(notBefore, dtf);
            notBeforeDate = Date.valueOf(ld);
        }

        if (StringUtils.isNotBlank(notAfter)) {
            LocalDate ld = LocalDate.parse(notAfter, dtf);
            notAfterDate = Date.valueOf(ld);
        }

        if (expiresInDays > 0) {
            LocalDate from = LocalDate.now();
            LocalDate to = LocalDate.now().plusDays(expiresInDays);
            notBeforeDate = Date.valueOf(from);
            notAfterDate = Date.valueOf(to);
        }

        if (StringUtils.isNotBlank(search)) {
            CertSearchParam param1 = new CertSearchParam(CertSearchParams.CertField.SUBJECT,
                    search, CertSearchParams.CertSearchParamRelation.CONTAINS);

            andParam.addSearchParam(param1);
        }

        SearchHits<CachedCertificate> search1;
        PageRequest pageRequest = PageRequest.of(page, pageSize);

        if (andParam.searchParamCount() == 0) {
            CertSearchParam param1 = new CertSearchParam(CertSearchParams.CertField.EXPIRES_ON,
                    ZonedDateTime.now(), CertSearchParams.CertSearchParamRelation.GREATER_THAN);
            search1 = cachedCertificateService.search(param1, sortBuilder, pageRequest);
        } else if (andParam.searchParamCount() == 1) {
            CertSearchParam single = andParam.getParams().get(0);
            search1 = cachedCertificateService.search(single, sortBuilder, pageRequest);
        } else {
            search1 = cachedCertificateService.search(andParam, sortBuilder, pageRequest);
        }

        List<CachedCertificate> items = search1.stream()
                .map(h -> h.getContent())
                .collect(Collectors.toList());
        return new PageImpl<>(items, pageRequest, search1.getTotalHits());
    }

    @GetMapping("/certificatesForServer/{serverId}")
    @Transactional
    public List<CachedCertificate> getCertificatesForServer(@PathVariable Long serverId) {
        Optional<ServerEntry> optionalServer = serverEntryRepository.findById(serverId);
        if (optionalServer.isPresent()) {
            ServerEntry serverEntry = optionalServer.get();
            List<CertificateRequest> certsForServer = certificateRequestRepository.findAllByServerEntryAndIssuedCertificateIsNotNull(serverEntry);

            Map<String, List<CertificateRequest>> certsByCaName = certsForServer.stream()
                    .collect(Collectors.groupingBy(c -> c.getCertAuthorityName()));

            List<CachedCertificate> all = new ArrayList<>();

            for (Map.Entry<String, List<CertificateRequest>> entry : certsByCaName.entrySet()) {
                List<String> serials = entry.getValue().stream()
                        .filter(r -> r.getIssuedCertificateSerial() != null)
                        .map(CertificateRequest::getIssuedCertificateSerial)
                        .collect(Collectors.toList());

                CertSearchParam param1 = CertSearchParam.createNew();
                param1.setField(CertSearchParams.CertField.SERIAL);
                param1.setRelation(CertSearchParams.CertSearchParamRelation.CONTAINS);
                param1.setValue(serials);

                CertSearchParam param2 = CertSearchParam.createNew();
                param2.setField(CertSearchParams.CertField.CA_NAME);
                param2.setRelation(CertSearchParams.CertSearchParamRelation.EQUALS);
                param2.setValue(entry.getKey());

                CertSearchParam andParam = CertSearchParam.createNew();
                andParam.setRelation(CertSearchParams.CertSearchParamRelation.AND);
                andParam.addSearchParam(param1);
                andParam.addSearchParam(param2);

                //todo use andParam
                SearchHits<CachedCertificate> search = cachedCertificateService.search(andParam);

                List<CachedCertificate> items = search.stream()
                        .map(h -> h.getContent())
                        .collect(Collectors.toList());

                all.addAll(items);
            }

            return all;
        }
        return new ArrayList<>();
    }

    public static Specification<CachedCertificate> buildSearch(final String text, boolean archived, boolean nonArchived,
                                                               boolean includeValid, boolean includeExpired, Date notBefore, Date notAfter) {

        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            Predicate onlyLatest = builder.equal(root.get("latestForDn"), true);
            predicates.add(onlyLatest);

            if (notBefore != null) {
                Predicate notBeforeFilter = builder.greaterThanOrEqualTo(root.get("validTo"), notBefore);
                predicates.add(notBeforeFilter);
            }

            if (notAfter != null) {
                Predicate notAfterFilter = builder.lessThanOrEqualTo(root.get("validTo"), notAfter);
                predicates.add(notAfterFilter);
            }

            if (StringUtils.isNotBlank(text)) {
                String finalText = text;
                if (!text.contains("%")) {
                    finalText = "%" + text + "%";
                }

                predicates.add(builder.like(root.get("dn"), finalText));
            }


            if (!includeValid || !includeExpired) {
                if (includeValid) {
                    Predicate validSearch = builder.greaterThanOrEqualTo(root.get("validTo"), Date.valueOf(LocalDate.now()));
                    predicates.add(validSearch);
                }

                if (includeExpired) {
                    Predicate expiredSearch = builder.lessThanOrEqualTo(root.get("validTo"), Date.valueOf(LocalDate.now()));
                    predicates.add(expiredSearch);
                }
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };

    }

}
