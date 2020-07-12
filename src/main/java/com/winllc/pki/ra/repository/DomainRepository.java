package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.Domain;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface DomainRepository extends BaseRepository<Domain> {
    List<Domain> findAllByBaseContains(String search);
    //List<Domain> findAllByCanIssueAccountsContains(Account account);
    List<Domain> findAllByIdIn(Collection<Long> ids);
    Optional<Domain> findDistinctByBaseEquals(String base);
}
