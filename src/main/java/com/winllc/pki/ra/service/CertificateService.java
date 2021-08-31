package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.CachedCertificate;
import com.winllc.acme.common.domain.CertificateRequest;
import com.winllc.acme.common.domain.ServerEntry;
import com.winllc.acme.common.repository.CachedCertificateRepository;
import com.winllc.acme.common.repository.CertificateRequestRepository;
import com.winllc.acme.common.repository.ServerEntryRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;
import java.time.LocalDate;
import java.sql.Date;
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

    private final CachedCertificateRepository cachedCertificateRepository;
    private final CertificateRequestRepository certificateRequestRepository;
    private final ServerEntryRepository serverEntryRepository;

    public CertificateService(CachedCertificateRepository cachedCertificateRepository,
                              CertificateRequestRepository certificateRequestRepository,
                              ServerEntryRepository serverEntryRepository) {
        this.cachedCertificateRepository = cachedCertificateRepository;
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
        Sort sort;
        if (order.equalsIgnoreCase("asc")) {
            sort = Sort.by(sortBy).ascending();
        } else {
            sort = Sort.by(sortBy).descending();
        }

        Pageable paging = PageRequest.of(page, pageSize, sort);

        Date notBeforeDate = null;
        Date notAfterDate = null;

        if(StringUtils.isNotBlank(notBefore)){
            LocalDate ld = LocalDate.parse(notBefore, dtf);
            notBeforeDate = Date.valueOf(ld);
        }

        if(StringUtils.isNotBlank(notAfter)){
            LocalDate ld = LocalDate.parse(notAfter, dtf);
            notAfterDate = Date.valueOf(ld);
        }

        if(expiresInDays > 0){
            LocalDate from = LocalDate.now();
            LocalDate to = LocalDate.now().plusDays(expiresInDays);
            notBeforeDate = Date.valueOf(from);
            notAfterDate = Date.valueOf(to);
        }

            Page<CachedCertificate> results = cachedCertificateRepository.findAll(
                    buildSearch(search, includeArchive, includeNonArchive, includeValid, includeExpired, notBeforeDate, notAfterDate),
                    paging);

            List<CachedCertificate> items = results.get()
                    .collect(Collectors.toList());
            return new PageImpl<>(items, results.getPageable(), results.getTotalElements());
    }

    @GetMapping("/certificatesForServer/{serverId}")
    @Transactional
    public List<CachedCertificate> getCertificatesForServer(@PathVariable Long serverId){
        Optional<ServerEntry> optionalServer = serverEntryRepository.findById(serverId);
        if(optionalServer.isPresent()) {
            ServerEntry serverEntry = optionalServer.get();
            List<CertificateRequest> certsForServer = certificateRequestRepository.findAllByServerEntryAndIssuedCertificateIsNotNull(serverEntry);

            Map<String, List<CertificateRequest>> certsByCaName = certsForServer.stream()
                    .collect(Collectors.groupingBy(c -> c.getCertAuthorityName()));

            List<CachedCertificate> all = new ArrayList<>();

            for(Map.Entry<String, List<CertificateRequest>> entry : certsByCaName.entrySet()){
                List<Long> serials = entry.getValue().stream()
                        .filter(r -> r.getIssuedCertificateSerial() != null)
                        .map(r -> Long.valueOf(r.getIssuedCertificateSerial()))
                        .collect(Collectors.toList());

                List<CachedCertificate> temp = cachedCertificateRepository.findAllBySerialInAndCaNameEquals(serials, entry.getKey());
                all.addAll(temp);
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

            if(notBefore != null){
                Predicate notBeforeFilter = builder.greaterThanOrEqualTo(root.get("validTo"), notBefore);
                predicates.add(notBeforeFilter);
            }

            if(notAfter != null){
                Predicate notAfterFilter = builder.lessThanOrEqualTo(root.get("validTo"), notAfter);
                predicates.add(notAfterFilter);
            }

            if (StringUtils.isNotBlank(text)) {
                String finalText = text;
                if (!text.contains("%")) {
                    finalText = "%" + text + "%";
                }

                predicates.add( builder.like(root.get("dn"), finalText));
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
