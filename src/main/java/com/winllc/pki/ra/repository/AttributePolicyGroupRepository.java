package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.AttributePolicyGroup;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface AttributePolicyGroupRepository extends BaseRepository<AttributePolicyGroup> {

    List<AttributePolicyGroup> findAllByAccount(Account account);
    Optional<AttributePolicyGroup> findDistinctByName(String name);
}
