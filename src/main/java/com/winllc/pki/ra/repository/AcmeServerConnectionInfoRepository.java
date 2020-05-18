package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.AcmeServerConnectionInfo;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
@Transactional
public interface AcmeServerConnectionInfoRepository extends BaseRepository<AcmeServerConnectionInfo> {
    AcmeServerConnectionInfo findByName(String name);
}
