package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.constants.AuditRecordType;
import com.winllc.pki.ra.domain.AuditRecord;
import com.winllc.pki.ra.domain.AuthCredential;
import com.winllc.pki.ra.domain.AuthCredentialHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface AuthCredentialRepository extends PagingAndSortingRepository<AuthCredential, Long> {
    List<AuthCredential> findAll();

    Optional<AuthCredential> findDistinctByKeyIdentifier(String kid);
    List<AuthCredential> findAllByParentEntity(AuthCredentialHolder holder);
    List<AuthCredential> findAllByParentEntityAndValidEquals(AuthCredentialHolder holder, Boolean valid);
}
