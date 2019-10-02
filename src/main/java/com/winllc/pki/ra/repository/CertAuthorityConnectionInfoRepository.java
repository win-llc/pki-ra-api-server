package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.AcmeServerConnectionInfo;
import com.winllc.pki.ra.domain.CertAuthorityConnectionInfo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CertAuthorityConnectionInfoRepository extends CrudRepository<CertAuthorityConnectionInfo, Long> {
    List<CertAuthorityConnectionInfo> findAll();
    CertAuthorityConnectionInfo findByName(String name);
}
