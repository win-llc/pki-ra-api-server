package com.winllc.pki.ra.repository;

import com.winllc.acme.common.domain.CertAuthorityConnectionProperty;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
@Transactional
public interface CertAuthorityConnectionPropertyRepository extends BaseRepository<CertAuthorityConnectionProperty> {

}
