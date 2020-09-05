package com.winllc.pki.ra.repository;

import com.winllc.acme.common.domain.CertAuthorityConnectionInfo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface CertAuthorityConnectionInfoRepository extends BaseRepository<CertAuthorityConnectionInfo> {
    @Query("select name from CertAuthorityConnectionInfo")
    List<String> findAllNames();
    Optional<CertAuthorityConnectionInfo> findByName(String name);
}
