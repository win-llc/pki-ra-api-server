package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.CertificateRequest;
import com.winllc.pki.ra.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateRequestRepository extends CrudRepository<CertificateRequest, Long> {

    List<CertificateRequest> findAll();
    List<CertificateRequest> findAllByStatusEquals(String status);
    List<CertificateRequest> findAllByRequestedByEquals(User user);
}