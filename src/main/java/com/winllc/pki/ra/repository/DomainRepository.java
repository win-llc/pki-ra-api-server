package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.Domain;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface DomainRepository extends CrudRepository<Domain, Long> {
    List<Domain> findAll();
    List<Domain> findAllByBaseContains(String search);
    List<Domain> findAllByCanIssueAccountsContains(Account account);
    List<Domain> findAllByIdIn(Collection<Long> ids);
}
