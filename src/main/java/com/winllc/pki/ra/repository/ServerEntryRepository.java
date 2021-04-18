package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.ServerEntry;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public interface ServerEntryRepository extends UniqueEntityRepository<ServerEntry> {

    Optional<ServerEntry> findDistinctByFqdnEquals(String fqdn);
    List<ServerEntry> findAllByAccount(Account account);
    List<ServerEntry> findAllByAccountId(Long id);
    Optional<ServerEntry> findDistinctByFqdnEqualsAndAccountEquals(String fqdn, Account account);
    Optional<ServerEntry> findDistinctByDistinguishedNameIgnoreCaseAndAccount(String distinguishedName, Account account);
}
