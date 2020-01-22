package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.AcmeServerConnectionInfo;
import com.winllc.pki.ra.domain.CertAuthorityConnectionInfo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertAuthorityConnectionInfoRepository extends CrudRepository<CertAuthorityConnectionInfo, Long> {
    List<CertAuthorityConnectionInfo> findAll();
    Optional<CertAuthorityConnectionInfo> findByName(String name);
}
