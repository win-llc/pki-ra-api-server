package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.AccountRequest;
import com.winllc.pki.ra.domain.RevocationRequest;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface RevocationRequestRepository extends BaseRepository<RevocationRequest> {

    Optional<RevocationRequest> findDistinctByIssuerDnAndSerial(String issuer, String serial);
    List<RevocationRequest> findAllByStatus(String status);
}
