package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.AttributePolicy;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
@Transactional
public interface AttributePolicyRepository extends BaseRepository<AttributePolicy> {
}
