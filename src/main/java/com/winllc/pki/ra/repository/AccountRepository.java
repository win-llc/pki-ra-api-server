package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.Account;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends CrudRepository<Account, Long> {

    Account findByKeyIdentifierEquals(String kid);
}
