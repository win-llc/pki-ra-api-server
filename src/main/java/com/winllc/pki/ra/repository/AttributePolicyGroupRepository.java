package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.AttributePolicyGroup;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
@Transactional
public interface AttributePolicyGroupRepository extends BaseRepository<AttributePolicyGroup> {
}
