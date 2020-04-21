package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.AccountRequest;
import com.winllc.pki.ra.domain.ServerEntry;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServerEntryRepository extends BaseRepository<ServerEntry> {

    Optional<ServerEntry> findDistinctByFqdnEquals(String fqdn);
    List<ServerEntry> findAllByAccount(Account account);
    List<ServerEntry> findAllByAccountId(Long id);
    Optional<ServerEntry> findDistinctByFqdnEqualsAndAccount(String fqdn, Account account);

}
