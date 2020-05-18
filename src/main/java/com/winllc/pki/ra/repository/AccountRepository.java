package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.PocEntry;
import com.winllc.pki.ra.domain.User;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface AccountRepository extends BaseRepository<Account> {

    Optional<Account> findByKeyIdentifierEquals(String kid);
    List<Account> findAllByAccountUsersContains(User user);
    List<Account> findAllByAccountUsersContainsOrPocsIn(User user, Collection<PocEntry> pocEntries);
    void deleteByKeyIdentifierEquals(String kid);
}
