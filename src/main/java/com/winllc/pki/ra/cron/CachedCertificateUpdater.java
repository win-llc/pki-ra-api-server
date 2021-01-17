package com.winllc.pki.ra.cron;

import com.winllc.acme.common.CertSearchParam;
import com.winllc.acme.common.CertSearchParams;
import com.winllc.acme.common.CertificateDetails;
import com.winllc.acme.common.ca.CertAuthority;
import com.winllc.pki.ra.ca.LoadedCertAuthorityStore;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.CachedCertificate;
import com.winllc.pki.ra.domain.CertificateRequest;
import com.winllc.pki.ra.keystore.ApplicationKeystore;
import com.winllc.pki.ra.repository.CachedCertificateRepository;
import com.winllc.pki.ra.repository.CertificateRequestRepository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CachedCertificateUpdater {

    private static final Logger log = LogManager.getLogger(CachedCertificateUpdater.class);

    @Autowired
    private CachedCertificateRepository repository;
    @Autowired
    private CertificateRequestRepository certificateRequestRepository;
    @Autowired
    private LoadedCertAuthorityStore certAuthorityStore;

    @Scheduled(fixedDelay = 1000 * 60 * 60)
    @Transactional
    public void update() throws Exception {
        Set<CachedCertificate> toSave = new HashSet<>();
        for (CertAuthority ca : certAuthorityStore.getAllCertAuthorities()) {
            LocalDate mostRecent = getMostRecentCacheDate(ca);

            if (mostRecent.isBefore(LocalDate.now())) {
                CertSearchParam dateSearch = CertSearchParam.createNew();
                dateSearch.setField(CertSearchParams.CertField.VALID_ON);
                dateSearch.setRelation(CertSearchParams.CertSearchParamRelation.GREATER_THAN);
                dateSearch.setValue(mostRecent.atStartOfDay());
                List<CertificateDetails> search = ca.search(dateSearch);

                Map<String, List<CertificateDetails>> dnMap = search.stream()
                        .collect(Collectors.groupingBy(s -> s.getSubject()));

                for(Map.Entry<String, List<CertificateDetails>> entry : dnMap.entrySet()){
                    Set<CachedCertificate> updateSet = new HashSet<>();
                    List<CachedCertificate> existing = repository.findAllByDnEquals(entry.getKey());
                    //updateSet.addAll(existing);

                    List<CachedCertificate> newResults = entry.getValue().stream()
                            .map(r -> searchResultToCached(r, ca.getName()))
                            .filter(c -> !existing.contains(c))
                            .collect(Collectors.toList());

                    if(CollectionUtils.isNotEmpty(newResults)) {
                        for (CachedCertificate cached : newResults) {
                            Optional<CertificateRequest> requestOptional = certificateRequestRepository.findDistinctByIssuedCertificateSerialAndCertAuthorityName(
                                    cached.getSerial().toString(), ca.getName());

                            if (requestOptional.isPresent()) {
                                CertificateRequest request = requestOptional.get();
                                Hibernate.initialize(request.getAccount());
                                //cached.setAccount(request.getAccount());
                                cached.setAccount(request.getAccount());
                            }

                            updateSet.add(cached);
                        }

                        if (updateSet.size() > 0) {
                            updateSet.forEach(c -> c.setLatestForDn(false));
                            updateSet.stream()
                                    .sorted()
                                    .findFirst().ifPresent(c -> {
                                c.setLatestForDn(true);
                            });

                            toSave.addAll(updateSet);
                        }
                    }
                }
            }
        }

        if(toSave.size() > 0){
            for(CachedCertificate cert : toSave){
                repository.save(cert);
            }
        }
    }

    private CachedCertificate searchResultToCached(CertificateDetails cert, String caName){
        CachedCertificate cached = new CachedCertificate();
        cached.setIssuer(cert.getIssuer());
        cached.setCaName(caName);
        cached.setSerial(serialToLong(cert.getSerial()));
        cached.setDn(cert.getSubject());
        cached.setValidFrom(Timestamp.from(cert.getValidFrom().toInstant()));
        cached.setValidTo(Timestamp.from(cert.getValidTo().toInstant()));

        return cached;
    }

    private long serialToLong(String serial){
        if(serial.startsWith("0x")){
            return Long.parseLong(serial.replace("0x",""), 16);
        }else{
            return Long.getLong(serial);
        }
    }

    private LocalDate getMostRecentCacheDate(CertAuthority ca) throws Exception {
        Optional<CachedCertificate> latest = repository
                .findTopByCaNameOrderByValidFromDesc(ca.getName());
        if(latest.isPresent()){
            CachedCertificate cached = latest.get();
            return cached.getValidFrom().toLocalDateTime().toLocalDate();
        }else{
            try {
                X509Certificate caCert = (X509Certificate) ca.getTrustChain()[ca.getTrustChain().length - 1];
                return LocalDate.from(caCert.getNotBefore().toInstant().atZone(ZoneId.systemDefault()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return LocalDate.now().minusMonths(1);
        }

        //return LocalDate.now().minusDays(60);
    }

}
