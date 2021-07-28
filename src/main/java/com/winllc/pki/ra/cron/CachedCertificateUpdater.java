package com.winllc.pki.ra.cron;

import com.winllc.acme.common.CertSearchParam;
import com.winllc.acme.common.CertSearchParams;
import com.winllc.acme.common.CertificateDetails;
import com.winllc.acme.common.ca.CertAuthority;
import com.winllc.acme.common.ca.LoadedCertAuthorityStore;
import com.winllc.acme.common.domain.CachedCertificate;
import com.winllc.acme.common.domain.CertificateRequest;
import com.winllc.acme.common.repository.CachedCertificateRepository;
import com.winllc.acme.common.repository.CertificateRequestRepository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.security.cert.*;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

//@Component
public class CachedCertificateUpdater {

    private static final Logger log = LogManager.getLogger(CachedCertificateUpdater.class);

    private final CachedCertificateRepository repository;
    private final CertificateRequestRepository certificateRequestRepository;
    private final LoadedCertAuthorityStore certAuthorityStore;

    public CachedCertificateUpdater(CachedCertificateRepository repository,
                                    CertificateRequestRepository certificateRequestRepository, LoadedCertAuthorityStore certAuthorityStore) {
        this.repository = repository;
        this.certificateRequestRepository = certificateRequestRepository;
        this.certAuthorityStore = certAuthorityStore;
    }

    @Scheduled(fixedDelay = 1000 * 60 * 60)
    @Transactional
    public void update() throws Exception {
        Set<CachedCertificate> toSave = new HashSet<>();
        for (CertAuthority ca : certAuthorityStore.getAllCertAuthorities()) {
            updateCertStatuses(ca);

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
            repository.saveAll(toSave);
        }
    }

    public void addSingleCertificate(X509Certificate x509Certificate){

        try {
            Optional<CertAuthority> optionalCa = certAuthorityStore.getLoadedCertAuthorityByIssuerDN(x509Certificate.getIssuerDN());

            if(optionalCa.isPresent()) {
                long serial = x509Certificate.getSerialNumber().longValue();
                CertAuthority ca = optionalCa.get();
                CachedCertificate cached = new CachedCertificate();
                cached.setIssuer(x509Certificate.getIssuerDN().getName().replace(", ", ","));
                cached.setCaName(ca.getName());
                cached.setSerial(serial);
                cached.setDn(x509Certificate.getSubjectDN().getName());
                cached.setValidFrom(Timestamp.from(x509Certificate.getNotBefore().toInstant()));
                cached.setValidTo(Timestamp.from(x509Certificate.getNotAfter().toInstant()));
                cached.setStatus("VALID");

                Optional<CachedCertificate> optionalCert = repository.findDistinctByIssuerAndSerial(cached.getIssuer(), serial);
                if(optionalCert.isEmpty()){
                    repository.save(cached);
                }
            }else{
                log.error("Could not find a CA in the system that matches: "+x509Certificate.getIssuerDN());
            }

        } catch (Exception e) {
            log.error("Could not add X509 Cert", e);
        }
    }

    private void updateCertStatuses(CertAuthority ca){
        try {
            X509CRL crl = ca.getCrl();
            Set<? extends X509CRLEntry> revokedCertificates = crl.getRevokedCertificates();

            List<Long> revokedSerials = revokedCertificates.stream()
                    .map(c -> c.getSerialNumber())
                    .map(s -> s.longValue())
                    .collect(Collectors.toList());

            List<CachedCertificate> shouldBeRevoked = repository.findAllBySerialInAndCaNameEqualsAndStatusEquals(revokedSerials, ca.getName(), "VALID");

            for (CachedCertificate c : shouldBeRevoked) {
                c.setStatus("REVOKED");
            }

            if(CollectionUtils.isNotEmpty(shouldBeRevoked)){
                log.info("Marking entries revoked: "+ shouldBeRevoked);
                repository.saveAll(shouldBeRevoked);
            }
        }catch (Exception e){
            log.error("Could not update Cert Statuses for "+ca.getName(), e);
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
        cached.setStatus(cert.getStatus());

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
                log.error("Could not get CA trust chain", e);
            }
            //todo should be dynamic
            return LocalDate.now().minusMonths(1);
        }
    }

}
