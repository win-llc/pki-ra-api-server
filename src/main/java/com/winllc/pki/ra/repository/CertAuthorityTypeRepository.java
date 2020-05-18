package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.CertAuthorityType;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
@Transactional
public interface CertAuthorityTypeRepository extends BaseRepository<CertAuthorityType> {

}
