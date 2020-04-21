package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.PocEntry;
import com.winllc.pki.ra.domain.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends BaseRepository<Account> {

    Optional<Account> findByKeyIdentifierEquals(String kid);
    List<Account> findAllByAccountUsersContains(User user);
    List<Account> findAllByAccountUsersContainsOrPocsIn(User user, List<PocEntry> pocEntries);
}
