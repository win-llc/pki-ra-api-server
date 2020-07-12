package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.domain.DomainPolicy;

import java.util.List;

public interface DomainPolicyRepository extends BaseRepository<DomainPolicy> {
    List<DomainPolicy> findAllByTargetDomainEquals(Domain targetDomain);
}
