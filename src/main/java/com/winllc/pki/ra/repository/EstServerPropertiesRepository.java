package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.AcmeServerConnectionInfo;
import com.winllc.pki.ra.domain.EstServerProperties;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
@Transactional
public interface EstServerPropertiesRepository extends BaseRepository<EstServerProperties> {
    EstServerProperties findByName(String name);
}
