package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.CertificateRequest;
import com.winllc.pki.ra.domain.ServerEntry;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
@Transactional
public interface CertificateRequestRepository extends BaseRepository<CertificateRequest> {

    List<CertificateRequest> findAllByStatusEquals(String status);
    List<CertificateRequest> findAllByRequestedByEquals(String user);
    List<CertificateRequest> findAllByPublicKeyBase64Equals(String publicKey);
    List<CertificateRequest> findAllByServerEntry(ServerEntry serverEntry);
}
