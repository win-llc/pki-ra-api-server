package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.IssuedCertificate;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
@Transactional
public interface IssuedCertificateRepository extends BaseRepository<IssuedCertificate> {

}
