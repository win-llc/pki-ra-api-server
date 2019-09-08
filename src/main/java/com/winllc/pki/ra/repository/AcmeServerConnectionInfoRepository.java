package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.acme.AcmeServerConnection;
import com.winllc.pki.ra.domain.AcmeServerConnectionInfo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcmeServerConnectionInfoRepository extends CrudRepository<AcmeServerConnectionInfo, Long> {
    List<AcmeServerConnectionInfo> findAll();
    AcmeServerConnectionInfo findByName(String name);
}
