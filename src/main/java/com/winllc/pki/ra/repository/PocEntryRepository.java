package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.PocEntry;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface PocEntryRepository extends CrudRepository<PocEntry, Long> {

    List<PocEntry> findAll();
    List<PocEntry> findAllByAccount(Account account);
    List<PocEntry> findAllByEmailEquals(String email);
    List<PocEntry> findAllByEmailContains(List<String> email);
    void deleteAllByEmailInAndAccountEquals(List<String> emails, Account account);
}
