package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.AcmeServerConnectionInfo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AcmeServerConnectionInfoRepository extends CrudRepository<AcmeServerConnectionInfo, Long> {
}
