package com.winllc.pki.ra.repository;

import com.winllc.acme.common.domain.CertAuthorityConnectionProperty;
import com.winllc.pki.ra.domain.AppRole;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.Optional;

@Repository
@Transactional
public interface AppRoleRepository extends BaseRepository<AppRole> {

    Optional<AppRole> findFirstByNameEquals(String name);
}
