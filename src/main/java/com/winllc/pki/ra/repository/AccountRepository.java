package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.PocEntry;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface AccountRepository extends BaseRepository<Account> {

    Optional<Account> findDistinctByProjectName(String projectName);
    Optional<Account> findByKeyIdentifierEquals(String kid);
    List<Account> findAllByPocsIn(Collection<PocEntry> pocEntries);
    List<Account> findAllByPocsContaining(PocEntry poc);
    void deleteByKeyIdentifierEquals(String kid);
}
