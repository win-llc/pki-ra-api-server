package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.CachedCertificate;
import com.winllc.pki.ra.domain.IssuedCertificate;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface CachedCertificateRepository extends PagingAndSortingRepository<CachedCertificate, Long>, JpaSpecificationExecutor<CachedCertificate> {

    List<CachedCertificate> findAllByDnEquals(String dn);
    List<CachedCertificate> findAllByDnContainsIgnoreCase(String search);
    Optional<CachedCertificate> findDistinctByIssuerAndSerial(String issuer, long serial);
    Optional<CachedCertificate> findTopByCaNameOrderByValidFromDesc(String issuer);

    List<CachedCertificate> findAllBySerialInAndCaNameEqualsAndStatusEquals(List<Long> serials, String caName, String status);
    List<CachedCertificate> findAllBySerialInAndCaNameEquals(List<Long> serials, String caName);
}
