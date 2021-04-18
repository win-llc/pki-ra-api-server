package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.domain.DomainPolicy;

import java.util.List;
import java.util.Optional;

public interface DomainPolicyRepository extends BaseRepository<DomainPolicy> {
    List<DomainPolicy> findAllByTargetDomainEquals(Domain targetDomain);
    Optional<DomainPolicy> findDistinctByAccountAndTargetDomain(Account account, Domain domain);

    List<DomainPolicy> findAllByAccount(Account account);
}
