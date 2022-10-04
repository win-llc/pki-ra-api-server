package com.winllc.pki.ra.service;


import com.winllc.acme.common.domain.ManagedServer;
import com.winllc.acme.common.repository.ManagedServerRepository;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.util.DateUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Transactional
@RestController
@RequestMapping("/api/managedServer")
public class ManagedServerRestService {
    private static final Logger log = LogManager.getLogger(ManagedServerRestService.class);
    private final ManagedServerRepository managedServerRepository;

    @Value("${actionUrlBase}")
    private String actionUrlBase;
    @Value("${expirationWindow}")
    private int expirationWindow;


    public ManagedServerRestService(ManagedServerRepository managedServerRepository) {
        this.managedServerRepository = managedServerRepository;
    }

    @GetMapping("/getPaged")
    @Transactional
    public Page<ManagedServer> getPaged(@RequestParam(required = false) String search,
                                        @RequestParam(defaultValue = "0") Integer page,
                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                        @RequestParam(defaultValue = "dateLatestCertWillExpire") String sortBy,
                                        @RequestParam(defaultValue = "desc") String order,
                                        @RequestParam(defaultValue = "false") Boolean includeArchive,
                                        @RequestParam(defaultValue = "true") Boolean includeNonArchive,
                                        @RequestParam(defaultValue = "false") Boolean includeExpired,
                                        @RequestParam(defaultValue = "true") Boolean includeValid,
                                        @RequestParam(defaultValue = "") String notBefore,
                                        @RequestParam(defaultValue = "") String notAfter,
                                        @RequestParam(defaultValue = "") String notBeforeUpdated,
                                        @RequestParam(defaultValue = "") String notAfterUpdated,
                                        @RequestParam(defaultValue = "-1") Integer expiresInDays,
                                        @RequestParam(defaultValue = "false") Boolean onlyCurrentUser,
                                        @RequestParam(defaultValue = "false") Boolean usePaging,
                                        @RequestParam(defaultValue = "false") Boolean noOwners,
                                        @RequestParam(defaultValue = "0") Long forUser,
                                        Authentication authentication) {

        LocalDateTime notBeforeDate = DateUtil.isoTimestampToLocalDateTime(notBefore).orElse(null);
        LocalDateTime notAfterDate = DateUtil.isoTimestampToLocalDateTime(notAfter).orElse(null);
        LocalDateTime notBeforeUpdatedDate = DateUtil.isoTimestampToLocalDateTime(notBeforeUpdated).orElse(null);
        LocalDateTime notAfterUpdatedDate = DateUtil.isoTimestampToLocalDateTime(notAfterUpdated).orElse(null);

        Specification<ManagedServer> spec = buildSearch(search, includeArchive, includeNonArchive,
                includeValid, includeExpired,
                notBeforeDate, notAfterDate,
                notBeforeUpdatedDate, notAfterUpdatedDate);

        Sort.Direction direction = order.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, pageSize, direction, sortBy);

        PageImpl<ManagedServer> pageImpl;
        if (usePaging) {

            Page<ManagedServer> all = managedServerRepository.findAll(spec, pageable);
            pageImpl = new PageImpl<>(all.get().collect(Collectors.toList()), all.getPageable(), all.getTotalElements());
        } else {
            List<ManagedServer> all = managedServerRepository.findAll(spec);
            pageImpl = new PageImpl<>(all, Pageable.unpaged(), all.size());
        }

        return pageImpl;
    }


    @GetMapping("/getId/{id}")
    public ManagedServer getById(@PathVariable Long id, Authentication authentication) throws RAObjectNotFoundException {
        log.debug("Inside getById: " + id);
        Optional<ManagedServer> optionalServer = managedServerRepository.findById(id);

        if(optionalServer.isPresent()) {
            ManagedServer server = optionalServer.get();
            log.debug("Found server: " + server.getFqdn());

            return server;
        }else{
            throw new RAObjectNotFoundException(ManagedServer.class, id);
        }
    }


    public static Specification<ManagedServer> buildSearch(final String text, boolean archived, boolean nonArchived,
                                                           boolean includeValid, boolean includeExpired,
                                                           LocalDateTime notBefore, LocalDateTime notAfter,
                                                           LocalDateTime notBeforeUpdated, LocalDateTime notAfterUpdated) {

        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            //Predicate onlyLatest = builder.equal(root.get("latestForDn"), true);
            //predicates.add(onlyLatest);


            if (notBefore != null) {
                Predicate notBeforeFilter = builder.greaterThanOrEqualTo(
                        root.get("latestCertExpiresOn"), notBefore);
                predicates.add(notBeforeFilter);
            }

            if (notAfter != null) {
                Predicate notAfterFilter = builder.lessThanOrEqualTo(
                        root.get("latestCertExpiresOn"), notAfter);
                predicates.add(notAfterFilter);
            }

            if (StringUtils.isNotBlank(text)) {
                String finalText = text;
                if (!text.contains("%")) {
                    finalText = "%" + text + "%";
                }

                Predicate dnLike = builder.like(root.get("fqdn"), finalText);
                Predicate projectLike = builder.like(root.get("project"), finalText);
                Predicate orSearch = builder.or(dnLike, projectLike);
                predicates.add(orSearch);
            }

            /*
            if (archived && nonArchived) {
                Predicate archiveOr = builder.or(builder.equal(root.get("archived"), true), builder.equal(root.get("archived"), false));
                predicates.add(archiveOr);
            } else if (archived) {
                predicates.add(builder.equal(root.get("archived"), true));
            } else if (nonArchived) {
                predicates.add(builder.equal(root.get("archived"), false));
            }

             */

            if (!includeValid || !includeExpired) {
                if (includeValid) {
                    Predicate validSearch = builder.greaterThanOrEqualTo(root.get("latestCertExpiresOn"), LocalDateTime.now());
                    predicates.add(validSearch);
                }

                if (includeExpired) {
                    Predicate expiredSearch = builder.lessThanOrEqualTo(root.get("latestCertExpiresOn"), LocalDateTime.now());
                    predicates.add(expiredSearch);
                }
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };

    }

}
